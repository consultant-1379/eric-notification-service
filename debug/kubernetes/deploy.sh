#
# COPYRIGHT Ericsson 2021
#
#
#
# The copyright to the computer program(s) herein is the property of
#
# Ericsson Inc. The programs may be used and/or copied only with written
#
# permission from Ericsson Inc. or in accordance with the terms and
#
# conditions stipulated in the agreement/contract under which the
#
# program(s) have been supplied.
#

#!/bin/bash

# minimal install
# ===============
#
# namespace=quick (or whatever you want)
# KAFKA_ENABLED=false
# TLS_ENABLED=false
#
# download locally the desired Notification Service and ADP PG charts
#
# execute all steps
#

# production install
# =================
# namespace=<an existing namespace with Kafka, SIP-TLS, SM, etc>
# KAFKA_ENABLED=true
# TLS_ENABLED=true
#
# execute only the last step

# NOTE: when we install in existing real installation with DB already up
# we need to change the networkpolicy:
# eric-oss-common-base-eric-oss-notification-service-db-limit-internal-traffic
# add allow 'app.kubernetes.io/name: ns'

# to execute a step put "true" between the square brackets, otherwise leave them empty

PG_SECRET=eric-eo-database-pg-secret # currently hardcoded, being fixed in IDUN-34226
PG_SERVICE=eric-oss-notification-service-database-pg
PG_DB=oss-notification-service
NAMESPACE=sun
KAFKA_ENABLED=true
TLS_ENABLED=true
PG_VERSION="7.6.0+37"
NS_VERSION="1.0.0-322"

[ true ] && \
kubectl create namespace quick

[ true ] && \
kubectl create secret generic $PG_SECRET -n $NAMESPACE \
          --from-literal=custom-user=newpostgres --from-literal=custom-pwd=newpostgres \
          --from-literal=super-user=postgres --from-literal=super-pwd=postgres \
          --from-literal=metrics-user=postgres --from-literal=metrics-pwd=postgres \
          --from-literal=replica-user=postgres --from-literal=replica-pwd=postgres

[ true ] && \
helm install pg eric-data-document-database-pg-${PG_VERSION}.tgz --namespace $NAMESPACE --wait \
    --set nameOverride=$PG_SERVICE \
    --set global.security.tls.enabled=$TLS_ENABLED \
    --set credentials.kubernetesSecretName=$PG_SECRET \
    --set highAvailability.replicaCount=1 \
    --set persistentVolumeClaim.size=1Gi \
    --set postgresDatabase=$PG_DB

[ true ] && \
helm install --debug ns eric-oss-notification-service-${NS_VERSION}.tgz --namespace $NAMESPACE \
    --wait --timeout 600s \
    --set nameOverride=ns \
    --set annotations.'sidecar\.istio\.io/inject'='"true"' \
    --set global.security.tls.enabled=$TLS_ENABLED \
    --set global.serviceMesh.enabled=true \
    --set replicaCount=1 --set messaging.kafka.enabled=$KAFKA_ENABLED \
    --set persistence.host=$PG_SERVICE --set persistence.database=$PG_DB --set persistence.secret=$PG_SECRET
