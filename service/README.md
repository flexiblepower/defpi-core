# Service Library
This Java library to create services implements the basic functionality for a service to communicate. It consists of a abstract main class in which ``InterfaceHandlers`` can be set that are used in the creation of new sessions with other services.

The ``ConnectionFactory`` is an abstract class with methods for creating/removing subscribe and publish handlers. Implementations of the ``ConnectionFactory`` are the classes that are used in the handlers in the main class. Connection factories are loaded by using reflection and a custom Java annotation indicating which interface the factory implements: 
```
@Factory(interfaceName="Interface Name")
```
This way the main class does not need to have knowledge of the factories that are implemented but it will search for them in runtime when starting the service.

The classes ``SubscribeHandler`` and ``PublishHandler`` are the classes that are used to actually send messages to other services. The ``SubscribeHandler`` class has an abstract method ``receiveMessage`` with as argument an object that has the type of the protocol buffer class. The ``PublishHandler`` implements a ``publish`` method that classes that inherit from ``PublishHandler`` can use.

The ``Session`` class is a protocol buffer class that is used by the orchestrator to indicate that a service should start communicating with another service. 
