FROM java:alpine

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ENTRYPOINT ["java", "-jar", "/${project.artifactId}-${project.version}-jar-with-dependencies.jar"]