# Orchestrator
The orchestrator is the component responsible for all containers and communication links between containers. It also acts as proxy for the service repository and the message repository.

Communication with the orchestrator can be done via the REST API that is exposed. This is done in the ``org.flexiblepower.rest`` package. The logic itself of the orchestrator is situated in the ``org.flexiblepower.orchestrator`` package.

The REST API uses JSON for serialization and deserialization, googles Gson library is used to map Json to java objects and vice versa.

In the ``src/main/resources`` folder the web interface that demonstrates the orchestrator functionalities. This interface is written in html/css/javascript and uses the REST API.
