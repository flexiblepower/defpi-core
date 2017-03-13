FROM java:alpine

ADD packaged-jar-with-dependencies.jar domain.crt /

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]