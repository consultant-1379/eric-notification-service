OS=linux
ARCH=amd64
IMG_VERSION=1.0.0
HOST=http://notification-service:8080
USERS=27
RATE=27
RUNTIME=1m

.PHONY: all

all: build-img run-tests

build-img:
	docker build -t armdocker.rnd.ericsson.se/proj-eo/common/bob-notification-service-loadtest:latest .

run-tests:
	docker run --network tests_nsnet --rm armdocker.rnd.ericsson.se/proj-eo/common/bob-notification-service-loadtest:latest  locust -f locustfile.py --headless --host $(HOST) -u $(USERS) -r $(RATE) --only-summary --run-time $(RUNTIME) --stop-timeout 30 --csv=reports/results

#--network stubs_stubsnet
#--network tests_nsnet
push-img:
	docker push armdocker.rnd.ericsson.se/proj-eo/common/bob-notification-service-loadtest:$(IMG_VERSION)
	docker rmi armdocker.rnd.ericsson.se/proj-eo/common/bob-notification-service-loadtest:latest:$(IMG_VERSION)
