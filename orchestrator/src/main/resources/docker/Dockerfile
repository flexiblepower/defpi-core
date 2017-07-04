FROM java:alpine

ADD ${project.build.dir}/packaged-jar-with-dependencies.jar /

ENTRYPOINT ["java", "-jar", "/packaged-jar-with-dependencies.jar"]