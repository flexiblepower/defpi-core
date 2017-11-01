from . import DropbackInterfaceConnectionManager
from .V002.DropbackInterfaceV002ConnectionHandlerImpl import DropbackInterfaceV002ConnectionHandlerImpl
from .Sendonly.DropbackInterfaceSendonlyConnectionHandlerImpl import DropbackInterfaceSendonlyConnectionHandlerImpl
from .V001.DropbackInterfaceV001ConnectionHandlerImpl import DropbackInterfaceV001ConnectionHandlerImpl


class DropbackInterfaceConnectionManagerImpl(DropbackInterfaceConnectionManager):
    """ DropbackInterfaceConnectionManagerImpl
        Manages all connection handlers for the Echo Service service
        Generated by Python code generator for dEF-Pi at Oct 24, 2017 10:43:45 AM by coenvl

        NOTE: This file is generated as a stub, and has to be implemented by the user. Re-running the codegen plugin 
        will not change the contents of this file.

        Template by TNO, 2017 """

    def __init__(self, service):
        """ Auto-generated constructor building the manager for the provided service """
        self.__service = service

    def build_dropback_interface_v001(self, connection):
        """ Build version V001 of the connection handler """
        return DropbackInterfaceV001ConnectionHandlerImpl(self.__service, connection)

    def build_dropback_interface_sendonly(self, connection):
        """ Build version Sendonly of the connection handler """
        return DropbackInterfaceSendonlyConnectionHandlerImpl(self.__service, connection)

    def build_dropback_interface_v002(self, connection):
        """ Build version V002 of the connection handler """
        return DropbackInterfaceV002ConnectionHandlerImpl(self.__service, connection)
