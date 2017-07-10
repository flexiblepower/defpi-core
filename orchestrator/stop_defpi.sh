#!/bin/sh
docker service rm orchestrator
docker service rm mongo

docker network rm orchestrator
