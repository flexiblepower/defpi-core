FROM hypriot/rpi-java:jre-1.8.111

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ADD log4j2.xml /

COPY def-pi-cacert.crt $JAVA_HOME/jre/lib/security

# TODO this doesn't work if you are building on an x86 machine, forget it for now...
#RUN \
#    cd $JAVA_HOME/jre/lib/security \
#    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias defpi -file def-pi-cacert.crt

ENTRYPOINT ["java", "-Dlog4j.configurationFile=/log4j2.xml", "-jar", "/${project.artifactId}-${project.version}-jar-with-dependencies.jar"]