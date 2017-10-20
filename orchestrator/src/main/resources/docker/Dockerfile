FROM java:alpine

ENV BUILD_TIMESTAMP ${maven.build.timestamp}
ENV BUILD_USER ${user.name}

COPY def-pi-cacert.crt $JAVA_HOME/jre/lib/security

RUN \
    cd $JAVA_HOME/jre/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias defpi -file def-pi-cacert.crt

COPY ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ENTRYPOINT java -jar $JVM_ARGUMENTS /${project.artifactId}-${project.version}-jar-with-dependencies.jar