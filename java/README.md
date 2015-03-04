Current examples:

 * `src/main/java/com/intellinote/sample/CreateUser.java` - demonstrates (1) user-less authentication, (2) creating a user and (3) accessing that user's account to create organizations, etc.

To run these examples you can do one of the following:

1. Build the examples with Maven using the provided `pom.xml` file and then run it with the command `java -cp target/sample-code-1.0.jar com.intellinote.sample.CreateUser`.

2. Make sure that the following jars are in your classpath and just run `com.intellinote.sample.CreateUser`.
	* 	httpclient-4.1.1.jar (Apache's Http Client)
 	* 	httpcore-4.1.jar (dependency of httpclient)
 	* 	gson-2.3.1.jar (Google's Json Parser)
 	* 	commons-logging-1.1.1.jar (dependency of gson)
 	* 	commons-codec-1.4.jar (dependency of gson)