#!/bin/sh
docker service rm orchestrator
docker service rm mongo
docker service rm orchestrator-ui

docker network rm orchestrator
