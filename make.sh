#!/bin/bash
MAVEN_OPTS="-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Djava.awt.headless=true -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
MAVEN_CLI_OPTS="-T 4 --batch-mode --errors --fail-at-end --show-version -DinstallAtEnd=true -DdeployAtEnd=true"

mvn $MAVEN_CLI_OPTS -f master/pom.xml test
