# Maven Parent Pom
To streamline the usage of the Maven Plugin and plugins for creating protocol buffer files and packaging java into a docker image.

The POM has two profiles: default and codegen.
## Default profile
The default profile is used for compiling and packaging the service. This profile will compile the service and then build the Docker image and push it to the registry.

A fat JAR is created to make sure all dependencies are available in the Docker image. This JAR will then be copied into the Docker image.

The docker-maven-plugin from Spotify is used to actually build and push the image.  

## Codegen profile
The codegen profile is used to generate code by the Maven Plugin and the protocol buffer plugin. Our own plugin needs very littly configuration, as most configuration is done in the service YAML. For compiling the protocol buffers the steps are a bit more complicated. A blogpost from Volkan Yazici (http://vlkan.com/blog/post/2015/11/27/maven-protobuf/) is used to configure the several standard plugins to compile the protocol buffers.

The codegen profile only uses the phase ``generate-sources``. Therefore the maven command for this profile is:
```
mvn -P codegen generate-sources
```
