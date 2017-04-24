FROM larmog/armhf-alpine-java:jdk-8u73

ADD packaged-jar-with-dependencies.jar /



LABEL org.flexiblepower.serviceName="Echo Service" org.flexiblepower.interfaces="[{\"name\":\"Echo Interface\",\"cardinality\":1,\"autoConnect\":false,\"subscribeHash\":\"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252\",\"publishHash\":\"eefc3942366e0b12795edb10f5358145694e45a7a6e96144299ff2e1f8f5c252\"}]" org.flexiblepower.mappings="[]" org.flexiblepower.ports="[]"

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]