# Notification Service Developer Guide

---

The Notification Service provides a means to collect events that are published by other services/applications
on the Kafka message bus and report them as REST messages to subscribed clients.

Notifications are sent only to clients with at least one subscription matching the event.
The match is implemented by enforcing a set of criteria that is defined in the subscription itself.

Applications that are using the Notification Service can be either clients (registering subscriptions and receiving
notifications) and/or producers (submitting events into the Kafka message bus).

A `notification-service-api` library is available for both of them to help:

- Clients to use REST API to create, read, and delete subscriptions. They will receive notifications
  as POSTs in the URL they provided as `address` field of the relevant subscription.

- Producers to format and deliver events to the Kafka message bus.

## Using HTTPS
Notification Service works with HTTPS, and in order to enable these please refer to the following:

### TLS configuration
TLS provides improved security to Notification Service by providing an encrypted connection to an authenticated user.
To use https for requests, you must configure the `iam-cacert-secret` secret with the service root certificate chain:
- The default name of secret is `iam-cacert-secret` but this name can be changed in helm configuration
- The default name of the field inside the secret is `tls.crt` but this field name can be changed in helm configuration
- The certificate chain should be added to the `iam-cacert-secret` secret
- The secret can be updated at deployment time or after deployment (see note below)

Note: The certificate chain can be added while the service is running as updating the `iam-cacert-secret` can be done at deployment time or
afterwards. The deployment listens for changes to the `iam-cacert-secret` secret and will update the trust store and use the added certificates 
automatically.

### Configure CA secret
The default values of the secret are: `iam-cacert-secret` and `tls.crt` but these can be customized in the helm configuration. In the deployment.yaml file, there are two environment variables that can be edited to customize the name and field of the secret. See relevant section in deployment.yaml in the following:
```
- name: CERTIFICATE_TRUSTSTORE_NAME
  value: iam-cacert-secret
- name: CERTIFICATE_TRUSTSTORE_FIELD
  value: tls.crt
```
- To change the value `iam-cacert-secret`, update the value of the environment variable `CERTIFICATE_TRUSTSTORE_NAME` to your new value.
- To change the value `tls.crt`, update the value of the environment variable `CERTIFICATE_TRUSTSTORE_FIELD` to your new value.

Note: Updates made to a running deployment will cause the pods to restart.

To create the secret run the following command:

```
kubectl create secret tls <customized-secret-name> --cert=<bundle.crt> --key=<server.key> -n <namespace>
```
Include the service root certificate chain inside the bundle cert value for `--cert` to use https requests.

### mTLS configuration
To use https for mtls or dual tls requests, you must create a secret the `eric-oss-notifications-client-secret` client secret with the client certificate in PKCS12 format:
- The default name of secret is `eric-oss-notifications-client-secret` but this name can be changed in helm configuration
- The default name of the field inside the secret is `client.p12` but this field name can be changed in helm configuration
- The PKCS12 file should be added to the `eric-oss-notifications-client-secret` secret
- The secret can be updated at deployment time or after deployment (see note below)

Note: The PKCS12 file can be added while the service is running as updating the `eric-oss-notifications-client-secret` can be done at deployment time or
afterwards. The deployment listens for changes to the `eric-oss-notifications-client-secret` secret, updates the key store, and uses the added certificates
automatically.

### Configure client secret
The default values of the secret are: `eric-oss-notifications-client-secret` and `client.p12` but these can be customized in the helm configuration. In the deployment.yaml file, there are two environment variables that can be edited to customize: the name and field of the secret. See relevant section in deployment.yaml in the following:

```
- name: CERTIFICATE_KEYSTORE_NAME
  value: eric-oss-notifications-client-secret
- name: CERTIFICATE_KEYSTORE_FIELD
  value: client.p12
```
- To change the value `eric-oss-notifications-client-secret`, update the value of the environment variable `CERTIFICATE_KEYSTORE_NAME` to your new value.
- To change the value `client.p12`, update the value of the environment variable `CERTIFICATE_KEYSTORE_FIELD` to your new value.

Note: Updates made to a running deployment cause the pods to restart.

To create the client.p12 file with the certificates, run the following command:

```
openssl pkcs12 -export -out <pkcs12-file-name> -inkey <client-private-key> -in <client-certificate> -certfile <ca-certificate>
ie:
openssl pkcs12 -export -out client.p12 -inkey client.key -in client.crt -certfile cacert.crt
```

To create the secret run the following command:

```
kubectl create secret generic <customized-secret-name> --from-file=<root-to-client.p12> -n <namespace>
```

## Using OAuth2.0
To use OAuth2.0, the notification service needs to fetches the specific connection properties from Connected Systems.

For OAuth2.0, the Connected System Type must be 'AuthenticationSystems' and the Connected System Sub type must be 'OAuth2 client credentials'. 
The required Connection properties specified must be populated to add the new connected system which can then be retrieved by the notification 
service.

Several connection properties are required to use OAuth2.0:
- auth_type: Identify the authentication mechanism (set to OAuth2.0)
- auth_url: Authorization url to get the access token from the client application, e.g. https://example-url.com/auth/token
- client_id: Unique ID to identify the client application by the server during the OAuth authentication, e.g. example-id
- client_secret: A string password used to authorize the client application by the server, e.g. test123
- grant_type: Type of OAuth flow that will be used (set to client_credentials)
- auth_headers: JSON string containing the header information to be used for OAuth communication, e.g. {"Content-Type":"application/json"}
- auth_token_request: To identify the type of token request that needs to be sent. A value of "json" denotes that the token request will be of json format where the client_id, client_secret, grant_type will be sent as a json request. An empty value denotes that the OAuth token request follows the standard as defined by RFC 6749.