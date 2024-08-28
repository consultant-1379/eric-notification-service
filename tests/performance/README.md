# Performace test for  Notification Service

## Design of the tests
  For the Notifications Service,
  we want to test load on which our service can handle REST quest
  in the Service we have two end points

    /notification/v1/subscriptions
      create/delete
### Running the tests
#### Locally run  test
  if locust is already installed on the developer machine,
  the following command can be used
 `` locust -f locustfile.py  --run-time 5m``
 locust ui can be reach by going to localhost:8089

 if the developer want to run the test locally in headless mode the following command can be used

 ``locust -f --headless --host notification-service_URL -u AMOUT_OF_USERS -r RATE --only-summary --run-time 5m --csv=reports/results``

 this will put the remote in to folder called reports

 #### Locally run against docker compose
 use the make file along with the docker compose included in the root tests folder ``eric-notification-service\tests``

 ``make -f Makerfile run-tests``

### Creating update to the performance tests
the main load test code is housed in the locustfile.py
any updates to test will result in a new build of the docker image

to create new docker image run the make file

``make -f Makerfile all``

followed by

``make -f Makerfile push img``
