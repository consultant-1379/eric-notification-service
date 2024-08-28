# About Notification Service

A notification can be defined in general as a piece of conclusive information passed to a specific receiver about the occurrence of an event in an application. A notification can be in the form of an email, message push, text to chat system, or a trigger to open a ticket. Most of the time, a notification does not require a response or acknowledgement to the sender.

The Ericsson Notification Service (ENS) provides a way to collect events published by other applications on a message bus. The events that are read from the message bus are processed and transformed into notifications. The notifications are reported as REST messages to a set of subscribed clients. 

Client applications can register themselves to ENS by creating "subscriptions". Notifications are sent only to clients which have at least one subscription matching an event through a set of criteria defined in the subscription itself.