    def {{vitf.version.builder}}(self, connection: Connection) -> {{vitf.handler.interface}}:
        """ Build version {{vitf.version}} of the connection handler """
        return {{vitf.handler.class}}(connection, self.service)
