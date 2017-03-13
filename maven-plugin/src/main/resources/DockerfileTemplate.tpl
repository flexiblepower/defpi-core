FROM {{from}}

ADD packaged-jar-with-dependencies.jar /

{{ports}}

LABEL org.flexiblepower.serviceName="{{name}}" org.flexiblepower.interfaces="{{interfaces}}" org.flexiblepower.mappings="{{mappings}}" org.flexiblepower.ports="[{{portsLabel}}]"

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]