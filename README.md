# dEF-Pi
This project contains the Java implementation of the Distributed Energy Flexibility Platform and Interface.

Current build status:
[![build status](https://ci.tno.nl/gitlab/FAN/def-pi/badges/master/build.svg)](https://ci.tno.nl/gitlab/FAN/def-pi/commits/master)

We use maven to integrate all projects, and as such, the following command can be used to compile the projects:

```
mvn -f master/pom.xml install
```

The project is separated in the following sub projects:

- master
- service-parent
- api
- commons
- codegen-common
- maven-plugin
- orchestrator
- service
- dashboard-gateway

## Maven Parent POM
The main maven project object model defines the settings that are used for all defpi projects. It defines where to get and where to push the modules as well as the java version and file encoding, and the plugins and dependencies to use.

#### Service-parent
A special sub-project of the master POM is the service parent. The service parent functions as a parent POM for all the services. Any developer implementing a service will have to use it as the parent to apply all functionality of the dEF-Pi platform. This includes the code-generation plugin and the service library for communication with the orchestrator. More information about this in the relevant sections.

A fat JAR is created using the maven-assembly-pluing to make sure all dependencies are available in one JAR file. The docker-maven-plugin from Spotify is used to actually build and push the image. On the *package* phase the plugin will build the docker image using the compiled jar, and in the *deploy* pushes it to the registry.

## API
The API project contains the API specification used in the Orchestrator. Using the [Swagger framework](https://swagger.io), enabling the development accross the entire API lifecycle, including design and documentation.

The packages used in the API project are:

* `org.flexiblepower.api` describing the endpoints of the API.
* `org.flexiblepower.exceptions` describing the exceptions that can be thrown by the API.
* `org.flexiblepower.model` containing the data objects that act as input or output of the API.

## Commons
The Commons project contains code shared accross multiple sub-projects. The shared code focuses on the communication and serialization of messages between the orchestrator and processes.

## Codegen-common
The Codegen Commons project contains the shared code for the Java and Python code generation projects. Providing the parsing of Service Descriptions (``service.json``) and a template engine.

## Defpi-maven-plugin
This maven plugin facilitates developers of dEF-Pi components. It does so by providing code that manages dEF-Pi connections and handles messages that are sent and received.
The only necessary input to this process is a service description where you specify a list of components and which types of messages are sent and received by each component. Also, a link to the message format is provided.

This file ``service.json`` must be placed in the src/main/resources folder. The maven plugin generates protobuf sources as well as the XSD files will be compiled to java code, and the interfaces and factories are built that are used by the service library.

The code generation step only executes on demand; it is not bound by default to a maven phase. The reason is to avoid overwriting code, in case the user wants to change interface, *even though this is not recommended!*.
To use the code generation first prepare a valid ``service.json`` file and then run the ``generate`` Maven goal, as follows:

```
mvn defpi:generate
```

## Orchestrator
The orchestrator is the component responsible for all containers and communication links between containers. It also acts as proxy for the service repository and the message repository.

he Orchestrator project contains implements the Orchestrator, used to manage Users, Services, Processes, Connections, and Nodes.

The package structure of the orchestrator is as follows:

* `org.flexiblepower.connectors` this package contains the connectors to components outside the orchestrator, the available connectors are:
  - `DockerConnector` to connect to the Docker Engine of the Docker Swarm manager.
  - `MongoDBConnector` to connect to the MongoDB store, containing information on: _Users_, _Processes_, and _Connections_. The MongoDB store also provides a proxy to Service descriptions.
  - `ProcessConnector` to connect to running _processes_.
  - `RegistryConnector` to connect to the _Registry_
* `org.flexiblepower.orchestrator` this package contains the _UserManager_, _ServiceManager_, and _NodeManager_.
* `org.flexiblepower.orchestrator.pendingchange` this package contains the _PendingChangeManager_, responsible for robustly applying changes to processes.
* `org.flexiblepower.process` this package contains the _ProcessManager_, responsible for the lifecycle management of processes.
* `org.flexiblepower.rest` provides a REST interface implementation of the API project, in order to communicate with the _Orchestrator_.

The REST API uses JSON for serialization and deserialization, jackson fasterxml library is used to map Json to java objects and vice versa.

## Service Library
The Service project contains the service library used for the implementation of Java services. Providing the communication with the _Orchestrator_ and integrates the implementation of a service.

## Dashboard-gateway
The dashboard gateway is a dEF-Pi service that acts as gateway for the dashboard. Allowing users to deploy different types of user interfaces for communicating with the orchestrator.
