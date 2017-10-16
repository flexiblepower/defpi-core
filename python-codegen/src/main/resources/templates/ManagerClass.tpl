from {{service.class}} import {{service.class}}

class {{itf.manager.class}}({{itf.manager.interface}}):
    """ {{itf.manager.class}}
        Manages all connection handlers for the {{service.name}} service
        Generated by {{generator}} at {{date}} by {{username}}
    
        NOTE: This file is generated as a stub, and has to be implemented by the user. Re-running the codegen plugin will 
        not change the contents of this file.
        
        Template by TNO, 2017 """
 
    def __init__(self, service):
        """ Auto-generated constructor building the manager for the provided service """
        self.__service = service
        
{{manager.implementations}}
