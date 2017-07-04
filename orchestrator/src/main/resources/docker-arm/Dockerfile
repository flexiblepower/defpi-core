FROM hypriot/rpi-java:jre-1.8.111

ADD packaged-jar-with-dependencies.jar /

COPY def-pi-cacert.crt $JAVA_HOME/jre/lib/security

# TODO this doesn't work if you are building on an x86 machine, forget it for now...
#RUN \
#    cd $JAVA_HOME/jre/lib/security \
#    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias defpi -file def-pi-cacert.crt

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]