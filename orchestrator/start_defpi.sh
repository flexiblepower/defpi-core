#!/bin/sh
./stop_defpi.sh

sleep 3

mkdir -p /tmp/mongo
docker network create --driver overlay orchestrator
docker service create --replicas 1 --network orchestrator --mount type=bind,source=/tmp/mongo,target=/data/db --name mongo mongo
docker service create --replicas 1 --name orchestrator-ui -e ORCHESTRATOR_URL=http://localhost:8080/ -p 8081:80 def-pi1.sensorlab.tno.nl:5000/defpi/orchestrator-ui
docker service create --replicas 1 --name orchestrator --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock -e MONGO_HOST=mongo -e MONGO_DATABASE=def-pi -e MONGO_PORT=27017 -e REGISTRY_URL=def-pi1.sensorlab.tno.nl:5000 -p 8080:8080 -p 9999:9999 --network orchestrator --dns 134.221.68.7 --dns 134.221.68.8 --dns 8.8.8.8 --dns 8.8.4.4 def-pi1.sensorlab.tno.nl:5000/defpi/orchestrator:0.2.1-SNAPSHOT
