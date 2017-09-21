# dEF-Pi
This project contains the java implementation of the Distributed Energy Flexibility Platform and Interface.

Current build status:
[![build status](https://ci.tno.nl/gitlab/FAN/def-pi/badges/master/build.svg)](https://ci.tno.nl/gitlab/FAN/def-pi/commits/master)

We use maven to integrate all projects, and as such, the following command can be used to compile the projects:

```
mvn -f master/pom.xml install
```

The project is separated in the following sub projects:

- master
- api
- commons
- orchestrator
- service
- maven-plugin

## Maven Parent POM
The main maven project object model defines the settings that are used for all defpi projects. It defines where to get and where to push the modules as well as the java version and file encoding, and the plugins and dependencies to use.

#### Service-parent
A special sub-project of the master POM is the service parent. The service parent functions as a parent POM for all the services. Any developer implementing a service will have to use it as the parent to apply all functionality of the dEF-Pi platform. This includes the code-generation plugin and the service library for communication with the orchestrator. More information about this in the relevant sections.

A fat JAR is created using the maven-assembly-pluing to make sure all dependencies are available in one JAR file. The docker-maven-plugin from Spotify is used to actually build and push the image. On the *package* phase the plugin will build the docker image using the compiled jar, and in the *deploy* pushes it to the registry.

## Orchestrator
The orchestrator is the component responsible for all containers and communication links between containers. It also acts as proxy for the service repository and the message repository.

Communication with the orchestrator can be done via the REST API that is exposed. This is done in the ``org.flexiblepower.rest`` package. The logic itself of the orchestrator is situated in the ``org.flexiblepower.orchestrator`` package.

The REST API uses JSON for serialization and deserialization, jackson fasterxml library is used to map Json to java objects and vice versa.

## Service Library
This Java library to create services implements the basic functionality for a service to communicate.

## Defpi-maven-plugin
This maven plugin facilitates developers of dEF-Pi components. It does so by providing code that manages dEF-Pi connections and handles messages that are sent and received.
The only necessary input to this process is a service description where you specify a list of components and which types of messages are sent and received by each component. Also, a link to the message format is provided. Currently EFI xsd and Protobuf messages are supported.
This file ``service.json`` must be placed in the src/main/resources folder. The maven plugin generates protobuf sources as well as the XSD files will be compiled to java code, and the interfaces and factories are built that are used by the service library.

The code generation step only executes on demand; it is not bound by default to a maven phase. The reason is to avoid overwriting code, in case the user wants to change interface, *even though this is not recommended!*.
To use the code generation first prepare a valid ``service.json`` file and then run the `generate` Maven goal, as follows:

```
mvn defpi:generate
```
