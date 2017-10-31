FROM java:alpine

ENV BUILD_TIMESTAMP ${maven.build.timestamp}
ENV BUILD_USER ${user.name}
ENV GIT_BRANCH ${git.branch}
ENV GIT_COMMIT ${git.commit.id}
ENV GIT_LOG ${git.commit.message.short}

EXPOSE 8080
HEALTHCHECK --timeout=3s CMD wget -qO - http://localhost:8080/diag || exit 1

COPY ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ENTRYPOINT java -jar $JVM_ARGUMENTS /${project.artifactId}-${project.version}-jar-with-dependencies.jar