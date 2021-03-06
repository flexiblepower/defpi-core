from defpi.Connection import Connection
from defpi.Service import Service

from .{{vitf.handler.interface}} import {{vitf.handler.interface}}
{{vitf.handler.imports}}


class {{vitf.handler.class}}({{vitf.handler.interface}}):
    """ {{vitf.handler.class}}
        Handles incoming messages from dEF-Pi connections
        Generated by {{generator}} at {{date}} by {{username}}

        NOTE: This file is generated as a stub, and has to be implemented by the user. Re-running the codegen plugin 
        will not change the contents of this file.

        Template by FAN, 2017 """
 
    def __init__(self, connection: Connection, service: Service):
        """ Auto-generated constructor building the manager for the provided service """
        self.service = service
        self.connection = connection

    def onSuspend(self):
        """ Called when the connection is suspended
            Auto-generated method stub """
        pass

    def resumeAfterSuspend(self):
        """ Called when the connection is being suspended
            Auto-generated method stub """
        pass
        
    def onInterrupt(self):
        """ Called when the connection is interrupted
            Auto-generated method stub """
        pass

    def resumeAfterInterrupt(self):
        """ Called when the connection is restored after an interruption
            Auto-generated method stub """
        pass

    def terminated(self):
        """ Called when the connection is terminated
            Auto-generated method stub """
        pass

{{vitf.handler.implementations}}
