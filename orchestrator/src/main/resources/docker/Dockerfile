FROM java:alpine

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ADD log4j2.xml /

COPY def-pi-cacert.crt $JAVA_HOME/jre/lib/security

RUN \
    cd $JAVA_HOME/jre/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias defpi -file def-pi-cacert.crt

ENTRYPOINT ["java", "-Xdebug", "-agentlib:jdwp=transport=dt_socket,address=9999,server=y,suspend=y", "-Dlog4j.configurationFile=/log4j2.xml", "-jar", "/${project.artifactId}-${project.version}-jar-with-dependencies.jar"]