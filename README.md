
## This code is written to check if kubectl -f commands can be achieved through io.fabric8 APIs 

This approach enables JAVA programs to not use exec or other methods to programmatically work with K8S.
When using any execute command approach, 'kubectl' binary becomes a dependency.
fabric8 APIs are very useful in removing this dependency and the java project can be run independently.

Test runner class for kubectl -f apply/delete/watch/portFwd using io.fabric8
Other classes like K8sSpecTemplate is used as is.

Class takes 3 arguments
* arg0 --> 100-spec-interpreter.yaml file path 
* arg1 --> properties file path used in template rendering
* args2 --> \<no-value\>|apply|delete. <no-value> will perform both apply and delete.
  Example spec file and property file is available under zeppelin-spec-handler/src/main/resources folder.
  
  For template rendering jinjava is used.

###### PortForward method is available in Kubectl.java but not invoked in this TestRunner. You can invoke this method if portForwarding is needed.


References: 
* https://github.com/apache/zeppelin/pull/3240
* https://github.com/fabric8io/kubernetes-client/tree/master/kubernetes-examples
* https://github.com/apache/spark/search?p=2&q=fabric8&unscoped_q=fabric8
* https://github.com/fabric8io/kubernetes-client/pull/1300

## How to build 
1. Clone Repositryu
2. Edit K8S_NAMESPACE (in KubectlTestRunner.java) and K8_URL(in Kubectl.java) as per K8S environment
3. RUN mvn clean package
4. kubectl.resourceList(result).createOrReplace(); only works with latest io.fabric8-kubernetes-client (master branch). [#1300](https://github.com/fabric8io/kubernetes-client/pull/1300) is merged to master branch I hope it ll be available in the next release of fabric8io/kubernetes-client.

## How to execute 
RUN 

`java -cp zeppelin-spec-handler-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.example.k8s.fabric8.KubectlTestRunner zeppelin-spec-handler/src/main/resources/100-interpreter-spec.yaml zeppelin-spec-handler/src/main/resources/test-spec.properties`
