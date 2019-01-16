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
import java.util.List;

import org.apache.log4j.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
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
	private static final String K8_URL = "https://k8s-apiserver.bcmt.cluster.local:8443";
	private KubernetesClient kubectl;

	public Kubectl(String spec, String namespace) throws IOException {
		this.spec = spec;
		this.namespace = namespace;
		this.kubectl = createK8sClient();
	}

	private KubernetesClient createK8sClient() {
		Config config = new ConfigBuilder().withNamespace(namespace).withMasterUrl(K8_URL).build();
		return new DefaultKubernetesClient(config);
	}

	public void apply() {
		List<HasMetadata> result = kubectl.load(new ByteArrayInputStream(spec.getBytes())).get();
		for (HasMetadata hasMetadata : result) {
			LOGGER.info("Applying below Resource : \n" + hasMetadata.toString() + "\n");
		}
		kubectl.resourceList(result).createOrReplace();
	}

	public void delete() {
		List<HasMetadata> result = kubectl.load(new ByteArrayInputStream(spec.getBytes())).get();
		kubectl.resourceList(result).delete();
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