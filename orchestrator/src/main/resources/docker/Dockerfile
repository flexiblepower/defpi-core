FROM java:alpine

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

COPY def-pi-cacert.crt $JAVA_HOME/jre/lib/security

RUN \
    cd $JAVA_HOME/jre/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias defpi -file def-pi-cacert.crt

ENTRYPOINT ["java", "-jar", "/${project.artifactId}-${project.version}-jar-with-dependencies.jar"]