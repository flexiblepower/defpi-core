MAVEN_CLI_OPTS=-T4 --batch-mode --errors --fail-at-end -DinstallAtEnd=true -DdeployAtEnd=true

.ONESHELL:

all: install

deploy:
	@export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
	mvn $(MAVEN_CLI_OPTS) -f master/pom.xml deploy

install-no-test:
	@export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
	mvn $(MAVEN_CLI_OPTS) -f master/pom.xml install -DskipTests

install:
	@export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
	mvn $(MAVEN_CLI_OPTS) -f master/pom.xml install

test:
	@export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
	mvn $(MAVEN_CLI_OPTS) -f master/pom.xml test

compile:
	@export MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
	mvn $(MAVEN_CLI_OPTS) -f master/pom.xml compile

dependencies:
	mvn -f master/pom.xml versions:display-dependency-updates versions:display-plugin-updates

clean:
	mvn -f master/pom.xml -T4 clean
