# Maven Plugin
The Maven plugin can be used to create a template for a service, so that a developer can create a new service very quickly.

## Configuration
### Folder structure
The following folder structure is needed for the plugin and 
```
.
├── src
│   └── main
│       ├── java
│       └── resources
│           └── service.yml
└── pom.xml
```

Protocol buffer files need to be saved in `src/main/resources`
### Component YAML
An component can be described with a YAML file, which is parsed by the plugin to create stubs for the component.
```
name: 'Echo Service'
protos:
  Message:
    url: 'http://efpi-rd1.sensorlab.tno.nl/protos/5c5822f44d5ec0a6b52a8afa397adb4b27f4af84d9ac55efafe2973a695b1096'
    type: 'Message'
interfaces:
  - name: 'Echo Interface'
    classPrefix: 'Echo'
    autoConnect: false
    cardinality: 0
    subscribe: Message
    publish: Message
```
This is the most basic service description that can be parsed. Protocol buffers can be loaded from the message repository, as shown in the example. Besides the message repository a protocol buffer can als be included by specifying a file. When a file is used the user also has an option to upload the message to the repository automatically.
 
### Pom file
In the pom file you need to specify your own groupid, artifactid and version. This will be used to create packages (*groupId*.*artifactId*, *groupId*.*artifactId*.handlers, *groupId*.*artifactId*.protos) and the image name for the Docker image (services/*artifactId*).
```
<project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.flexiblepower</groupId>
    <artifactId>service-parent</artifactId>
    <version>0.0.2</version>
  </parent>

  <groupId>org.flexiblepower</groupId>
  <artifactId>echo</artifactId>
  <version>0.0.1</version>

</project>
```
## Code generation
The stubs and the protocol buffers can be created by executing the following command in the root of the project:
```
mvn -P codegen generate-sources
```

## Publish
To publish an component execute the following command in the root of the project:
```
mvn clean package
```

