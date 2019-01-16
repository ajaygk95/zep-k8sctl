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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 
 * Test runner class for kubectl -f apply/delete/watch/portFwd using io.fabric8
 * Other classes like K8sSpecTemplate is used as is.
 *
 * Class takes 3 args. arg0 --> 100-spec-interpreter.yaml file path arg1 -->
 * properties file path used in template rendering args2 -->
 * <no-value>|apply|delete K8S_NAMESPACE (in KubectlTestRunner.java) and
 * K8_URL(in Kubectl.java) are hard-coded, change as needed.
 *
 *
 * In K8SRemoteInterpreterProcess.java when "start" method is called the apply
 * and portForward is being invoked. PortForward method is available in
 * Kubectl.java but not invoked in this TestRunner.
 *
 *
 * This testCode is written to check if kubectl as used in
 * https://github.com/apache/zeppelin/pull/3240 can be replaced with Java Api's.
 * References: https://github.com/apache/zeppelin/pull/3240
 * https://github.com/fabric8io/kubernetes-client/tree/master/kubernetes-examples
 * https://github.com/apache/spark/search?p=2&q=fabric8&unscoped_q=fabric8
 *
 */
public class KubectlTestRunner {
	private static final Logger LOGGER = Logger.getLogger(KubectlTestRunner.class);
	private static final String K8S_NAMESPACE = "zeppelin";

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			LOGGER.error("spec-template.yaml test-spec.properties not given");
			return;
		}
		KubectlTestRunner runner = new KubectlTestRunner();
		try {
			Properties properties = runner.readProperties(args[1]);
			File interpreter_spec_file = new File(args[0]);

			// Render yaml file
			K8sSpecTemplate specTemplate = new K8sSpecTemplate();
			specTemplate.loadProperties(properties);
			String spec = specTemplate.render(interpreter_spec_file);
			LOGGER.trace(spec);
			// Apply kubectl action
			String action = new String("all");
			if (args.length > 2 && !args[2].isEmpty()) {
				action = new String(args[2].equalsIgnoreCase("apply") ? "apply" : "delete");
			}
			LOGGER.info("action is: " + action);
			if (action.equals("all")) {
				runner.executeAction("apply", spec);
				runner.executeAction("delete", spec);
			} else {
				runner.executeAction(action, spec);
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	protected void executeAction(String action, String spec) throws Exception {
		Kubectl kubectl = new Kubectl(spec, K8S_NAMESPACE);
		switch (action) {
		case "apply":
			LOGGER.info("action-apply");
			kubectl.apply();
			break;
		case "delete":
			LOGGER.info("action-delete");
			kubectl.delete();
			break;
		default:
			LOGGER.error("No default case present");
			kubectl.close();
			break;
		}
		kubectl.wait("knobby-elk-zeppelin-6d94bf8b76-pk6k8");
	}

	protected Properties readProperties(String propFile) throws IOException {
		InputStream test_spec_prop = new FileInputStream(propFile);
		Properties properties = new Properties();
		properties.load(test_spec_prop);
		LOGGER.info("Properties: " + properties);
		return properties;
	}
}
