
* This code is written to check if kubectl -f commands can be achieved through io.fabric8 APIs *

This approach enables JAVA programs to not use exec or other methods to programmatically work with K8S.
When using any execute command approach, 'kubectl' binary becomes a dependency.
fabric8 APIs are very useful in removing this dependency and the java project can be run independently.

Test runner class for kubectl -f apply/delete/watch/portFwd using io.fabric8
Other classes like K8sSpecTemplate is used as is.

Class takes 2 args. arg0 --> 100-spec-interpreter.yaml file path arg1 -->
properties file path used in template rendering args2 -->
<no-value>|apply|delete K8S_NAMESPACE (in KubectlTestRunner.java) and
K8_URL(in Kubectl.java) are hard-coded, change as needed.


In K8SRemoteInterpreterProcess.java when "start" method is called the apply
and portForward is being invoked. PortForward method is available in
Kubectl.java but not invoked in this TestRunner.


This testCode is written to check if kubectl as used in
https://github.com/apache/zeppelin/pull/3240 can be replaced with Java Api's.
References: https://github.com/apache/zeppelin/pull/3240
https://github.com/fabric8io/kubernetes-client/tree/master/kubernetes-examples
https://github.com/apache/spark/search?p=2&q=fabric8&unscoped_q=fabric8

* How to build *
1. Clone Repositry
2. Edit K8S_NAMESPACE (in KubectlTestRunner.java) and K8_URL(in Kubectl.java) as per K8S environment
3. RUN mvn clean package

* How to execute *
