Current examples:

 * `src/main/java/com/intellinote/sample/CreateUser.java` - demonstrates (1) user-less authentication, (2) creating a user and (3) accessing that user's account to create organizations, etc.

To run these examples you can do one of the following:

* Build the examples with Maven using the provided `pom.xml` file 
	1. `mvn package`
	2. `mvn exec:java`

* Make sure that the following jars are in your classpath and just run `com.intellinote.sample.CreateUser`.  Any easy way to do this would be to modify the included `run.sh` or `run.bat` file to have the proper paths on your file system. 
	* 	httpclient-4.1.1.jar (Apache's Http Client)
 	* 	httpcore-4.1.jar (dependency of httpclient)
 	* 	gson-2.3.1.jar (Google's Json Parser)
 	* 	commons-logging-1.1.1.jar (dependency of gson)
 	* 	commons-codec-1.4.jar (dependency of gson)