/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.k8s.fabric8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.rbac.KubernetesRole;
import io.fabric8.kubernetes.api.model.rbac.KubernetesRoleBinding;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

public class Kubectl {
	private static final Logger LOGGER = Logger.getLogger(Kubectl.class);
	private final String spec;
	private final String namespace;
	private final Map<String, String> resourcesMap;
	private static final String K8_URL = "https://k8s-apiserver.bcmt.cluster.local:8443";
	private KubernetesClient kubectl;

	public Kubectl(String spec, String namespace) throws IOException {
		this.spec = spec;
		this.namespace = namespace;
		this.kubectl = createK8sClient();
		this.resourcesMap = createResources(this.spec);
		if (this.resourcesMap.isEmpty()) {
			LOGGER.info("No resources loaded from spec: " + this.spec);
			throw new NullPointerException();
		}
	}

	private Map<String, String> createResources(String spec) throws IOException {
		Map<String, String> rMap = new HashMap<String, String>();

		// Split the input spec to individual resource json which is used to
		// apply/delete the resource
		ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
		YAMLFactory yaml = new YAMLFactory();
		LOGGER.info("spec:" + spec);
		YAMLParser yamlParser = yaml.createParser(new ByteArrayInputStream(spec.getBytes()));
		List resList = yamlReader.readValues(yamlParser, new TypeReference<ObjectNode>() {
		}).readAll();

		for (Object resource : resList) {
			LOGGER.info("Resource : " + resource.toString());
			JsonNode rootNode = yamlReader.readTree(resource.toString().getBytes());
			JsonNode idNode = rootNode.path("kind");

			if (idNode.asText().equalsIgnoreCase("pod")) {
				rMap.put("pod", resource.toString());
				// YAMLMapper().writeValueAsString(resource));
			} else if (idNode.asText().equalsIgnoreCase("service")) {
				rMap.put("service", resource.toString());
			} else if (idNode.asText().equalsIgnoreCase("role")) {
				rMap.put("role", resource.toString());
			} else if (idNode.asText().equalsIgnoreCase("rolebinding")) {
				rMap.put("rolebinding", resource.toString());
			}

		}
		return rMap;
	}

	private KubernetesClient createK8sClient() {
		Config config = new ConfigBuilder().withNamespace(namespace).withMasterUrl(K8_URL).build();
		return new DefaultKubernetesClient(config);
	}

	public void apply() {

		for (String key : resourcesMap.keySet()) {
			InputStream recStream = new ByteArrayInputStream(resourcesMap.get(key).getBytes());
			if (key.equals("pod")) {
				Pod pod = (Pod) kubectl.load(recStream).get().get(0);
				LOGGER.debug("Pod is :" + pod.toString() + "\n");
				kubectl.pods().create(pod);
			} else if (key.equals("service")) {
				Service service = (Service) kubectl.load(recStream).get().get(0);
				LOGGER.debug("Service is :" + service.toString() + "\n");
				kubectl.services().create(service);
			} else if (key.equals("role")) {
				KubernetesRole role = kubectl.rbac().kubernetesRoles().load(recStream).get();
				LOGGER.debug("role: " + role.toString());
				kubectl.rbac().kubernetesRoles().create(role);
			} else if (key.equals("rolebinding")) {
				KubernetesRoleBinding roleBinding = kubectl.rbac().kubernetesRoleBindings().load(recStream).get();
				LOGGER.debug("roleBinding: " + roleBinding.toString());
				kubectl.rbac().kubernetesRoleBindings().create(roleBinding);
			}
		}

	}

	public void delete() {
		for (String key : resourcesMap.keySet()) {
			InputStream recStream = new ByteArrayInputStream(resourcesMap.get(key).getBytes());
			if (key.equals("pod")) {
				Pod pod = (Pod) kubectl.load(recStream).get().get(0);
				LOGGER.debug("Pod is :" + pod.toString() + "\n");
				kubectl.pods().delete(pod);
			} else if (key.equals("service")) {
				Service service = (Service) kubectl.load(recStream).get().get(0);
				LOGGER.debug("Service is :" + service.toString() + "\n");
				kubectl.services().delete(service);
			} else if (key.equals("role")) {
				KubernetesRole role = kubectl.rbac().kubernetesRoles().load(recStream).get();
				LOGGER.debug("role: " + role.toString());
				kubectl.rbac().kubernetesRoles().delete(role);
			} else if (key.equals("rolebinding")) {
				KubernetesRoleBinding roleBinding = kubectl.rbac().kubernetesRoleBindings().load(recStream).get();
				LOGGER.debug("roleBinding: " + roleBinding.toString());
				kubectl.rbac().kubernetesRoleBindings().delete(roleBinding);
			}
		}
	}

	public void portFwd(String podName, int port, int localPort) {
		kubectl.pods().withName(podName).portForward(port, localPort);
	}

	public void wait(String podName) {
		kubectl.pods().withName(podName).watch(new Watcher<Pod>() {
			@Override
			public void eventReceived(Action action, Pod resource) {
				LOGGER.info("PodName and Status: " + resource.getMetadata().getName() + " "
						+ resource.getStatus().getPhase());
				List<PodCondition> conditions = resource.getStatus().getConditions();
				for (PodCondition podCondition : conditions) {
					if (podCondition.getType().equalsIgnoreCase("Ready")) {
						if (podCondition.getStatus().equalsIgnoreCase("True")) {
							return;
						}
					}
				}

			}

			@Override
			public void onClose(KubernetesClientException cause) {
				// TODO Auto-generated method stub

			}
		});
	}

	public void close() {
		kubectl.close();
	}

}