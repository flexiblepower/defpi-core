CODEGEN_IMAGE=flexiblepower/defpi-codegen-python
SERVICE=$(shell grep -m1 "name" service.json | sed '{s/name//g; s/[\":,]//g}' | xargs | sed -r 's/ +/_/g' | awk '{print tolower($$0)}')
VERSION=$(shell grep -m1 "version" service.json | sed '{s/version//g; s/[\":,]//g}' | xargs | sed -r 's/ +/_/g' | awk '{print tolower($$0)}')
REGISTRY=localhost:5000
IMAGENAME=$(REGISTRY)/services/$(SERVICE)

all: generate

.PHONY: generate package deploy clean

generate: .generated
.generated: service.json
	@echo "Generating code for $(SERVICE)"
	docker run --rm -u$(shell id -u) -v$(shell pwd):/usr/local/defpi $(CODEGEN_IMAGE)
	@touch $@

package: .packaged
.packaged: .generated requirements.txt $(shell find service) $(shell find resources)
	@echo "packaging $(SERVICE)"
	docker build -f resources/docker/Dockerfile . -t $(IMAGENAME):$(VERSION)
	docker tag $(IMAGENAME):$(VERSION) $(IMAGENAME):latest
	@touch $@

deploy: .deployed
.deployed: .packaged
	@echo "Deploying $(SERVICE) to $(REGISTRY)"
	docker push $(IMAGENAME):$(VERSION)
	docker push $(IMAGENAME):latest
	@touch $@

clean:
	rm -f .packaged .generated .deployed
