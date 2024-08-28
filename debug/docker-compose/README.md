To launch notification service:

docker-compose up -d

The first time, the startup will load the applications into your local docker space, so it will be a bit longer. The next runs will be faster.
After startup, it's possible to use a client (e.g. postman, curl or your client application) to connect to notification service on port 8080.

To view log information from notification service:

log.sh ns

To close the session:

docker-compose down

After running docker-compose the first time, two sub-folders of . will be created:

./data : contains the postgres data
./zk-single-kafka-single : contains the Kafka/Zookeeper data

They guarantee the persistence of the data across testing sessions. If you want to start from scratch, delete them.
They are owned by root, so use:

sudo /bin/rm -rf data
sudo /bin/rm -rf zk-single-kafka-single

to get rid of them, as needed.

Use port 8001 to debug the notification service, as needed. Change the version of the notification service in docker-compose.yml
to the one you want to debug/test, as needed.

Using port 8080 is possible to send REST requests to the notification service via curl, postman or any other utilities.
This environment is missing an application to send events into Kafka and to receive notifications on the REST. 
Create producer and client according to your needs and add them to the docker-compose.yml. 

Use port 5432 to inspect the content of the database via available postgres database utilities.



