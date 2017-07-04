FROM larmog/armhf-alpine-java:jdk-8u73

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ENTRYPOINT ["java", "-jar", "/${project.artifactId}-${project.version}-jar-with-dependencies.jar"]