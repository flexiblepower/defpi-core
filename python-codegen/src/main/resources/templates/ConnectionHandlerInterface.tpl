from abc import abstractmethod

from defpi.ConnectionHandler import ConnectionHandler, InterfaceInfo


class {{vitf.handler.interface}}(ConnectionHandler):
    """ {{vitf.handler.interface}}
        Abstract base class for connection handlers
        Generated by {{generator}} at {{date}} by {{username}}

        NOTE: This file will be overwritten when the code generator is re-run; any user-made changes will be removed.

        Template by FAN, 2017 """

    interfaceInfo = InterfaceInfo(name="{{itf.name}}",
                                  version="{{vitf.version}}",
                                  receivesHash="{{vitf.receivesHash}}",
                                  sendsHash="{{vitf.sendsHash}}",
                                  serializer="{{vitf.serializer}}")

{{vitf.handler.definitions}}
