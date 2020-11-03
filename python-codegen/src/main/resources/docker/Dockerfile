FROM openjdk:11-jre-slim

WORKDIR /usr/local/defpi

RUN apt-get update && apt-get install -y --no-install-recommends \
		python3-pip \
		python3-setuptools \
	&& rm -rf /var/lib/apt/lists/* \\
	&& pip3 install --upgrade pip setuptools pyxb

COPY ${project.artifactId}-${project.version}-jar-with-dependencies.jar /

ENTRYPOINT java -jar $JVM_ARGUMENTS /${project.artifactId}-${project.version}-jar-with-dependencies.jar

