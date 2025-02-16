## Java test project
This is a test project showing how to call most of the functions in the OpenIAP core library.
`mainTestCli.java` is a command line interface that can be used to call a few functions, with the `t` command you can a test function that calls all calls all the functions in the core library inside `clienttestcli.java`

# Build and run

To build the project:
```bash
mvn clean package
```

To run the test application:
```bash
mvn package
java -jar target/test-client-0.0.2-jar-with-dependencies.jar
```
github repository:
```
https://github.com/skadefro/javatest.git
```