#!/bin/bash

#curl -u "333BF9DCEF782D44C580:NezFEKmv2BJrjnj7PwPSwEpo4jUkBmRjR5B8Qx4z" \
#-X POST \
#-H 'Accept: application/json' \
#-H 'Content-Type: application/json' \
#-d '{"inServiceStrategy":{"batchSize":1, "intervalMillis":2000, "startFirst":false, "launchConfig":{"kind":"container", "networkMode":"managed", "privileged":false, "publishAllPorts":false, "readOnly":false, "startOnCreate":true, "stdinOpen":false, "tty":false, "vcpu":1, "capAdd":[], "capDrop":[], "count":null, "cpuSet":null, "cpuShares":null, "dataVolumes":[], "dataVolumesFrom":[], "description":null, "devices":[], "dns":[], "dnsSearch":[], "domainName":null, "hostname":null, "imageUuid":"docker:efpi-rd1.sensorlab.tno.nl:5000/orchestrator", "labels":{"io.rancher.container.pull_image":"always", "io.rancher.scheduler.affinity:host_label":"node=master"}, "logConfig":{"config":{}, "driver":""}, "memory":null, "memoryMb":null, "memorySwap":null, "pidMode":null, "ports":["80:80/tcp"], "requestedIpAddress":null, "user":null, "userdata":null, "version":"21515f58-cbea-42e8-bdba-78eec4c11edf", "volumeDriver":null, "workingDir":null, "dataVolumesFromLaunchConfigs":[], "networkLaunchConfig":null, "type":"launchConfig", "createIndex":null, "created":null, "deploymentUnitUuid":null, "externalId":null, "firstRunning":null, "healthState":null, "removed":null, "startCount":null, "systemContainer":null, "uuid":null}, "secondaryLaunchConfigs":[]}, "toServiceStrategy":null}' \
#'http://efpi-rd1.sensorlab.tno.nl:8080/v1/projects/1a5/services/1s1/?action=upgrade'


#sleep 15

#curl -u "333BF9DCEF782D44C580:NezFEKmv2BJrjnj7PwPSwEpo4jUkBmRjR5B8Qx4z" \
#-X POST \
#-H 'Accept: application/json' \
#-H 'Content-Type: application/json' \
#-d '{}' \
#'http://efpi-rd1.sensorlab.tno.nl:8080/v1/projects/1a5/services/1s1/?action=finishupgrade'
