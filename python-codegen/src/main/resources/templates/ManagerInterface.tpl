from abc import abstractmethod

from defpi.ConnectionHandlerManager import ConnectionHandlerManager

{{itf.manager.imports.interface}}


class {{itf.manager.interface}}(ConnectionHandlerManager):
    """ {{itf.manager.interface}}
        Abstract base class for connection handler manager
        Generated by {{generator}} at {{date}} by {{username}}

        NOTE: This file will be overwritten when the code generator is re-run; any user-made changes will be removed.

        Template by FAN, 2017 """

{{itf.manager.definitions}}
