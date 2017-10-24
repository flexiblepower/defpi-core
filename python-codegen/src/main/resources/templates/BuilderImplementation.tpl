    def {{vitf.version.builder}}(self, connection):
        """ Build version {{vitf.version}} of the connection handler """
        return {{vitf.handler.class}}(self.__service, connection)