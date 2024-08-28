# Notification Service Use Cases

---

This section provides an overview of several common use cases for Notification Service (ENS).

## Create a Subscription

 With ENS, you can create new subscriptions. When creating a subscription, you do not need to specify a subscription id because a unique UUID is automatically assigned and returned by ENS after the operation completes successfully. ENS completes checks to ensure the new subscription is not a duplicate of a previous subscription created and also completes other checks to ensure it is created correctly.

## Delete a Subscription 

 With ENS, you can delete a subscription. The `subscriptionId` must be provided to identify the subscription that you are deleting. Trying to remove a non existing subscription results in an error.

## Get a Subscription

ENS can retrieve a specific subscription given a `subscriptionId`. If the subscription exists, the full subscription record is returned, otherwise an error is returned.

## Get All Subscriptions

ENS can retrieve all the subscriptions stored in the ENS database. 

## Send a Notification	

ENS reads all the incoming events from the Kafka message bus, finds the matching subscriptions, and then generates and dispatches the notifications to the relevant clients.

## SO Installation

ENS installation is added to the SO installation charts. 

## SO Upgrade

ENS is added to SO upgrade procedures.

## SO Backup and Restore 

ENS is added to BUR (Backup & Restore) procedures. The subscription database must be backed-up and restored.

