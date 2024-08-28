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

import logging
import random
from locust import HttpUser, task, TaskSet, events, constant

LOG = logging.getLogger(__name__)


def __init__(self):
    self.user_uuid = ''


class NotificationServiceLoadTest(TaskSet):

    @task
    def create_and_delete_subscription(self):
        payload = {
            "address": 'http://my.test.host' + str(random.randint(1, 2000000)) + '.com',
            "tenant": 'me',
            "subscriptionFilter": [
                {
                    "eventType": "ServiceOrderCreateEvent",
                    "filterCriteria": "event.priority=gt=1;event.priority=lt=10;(tenant==tenant1,"
                                      "tenant==tenant2)",
                    "fields": "event.eventType,event.eventId,event.priority"}
            ]
        }
        LOG.info("creating subscription")
        LOG.debug(f"creating subscription using {payload}")
        response = self.client.post("/notification/v1/subscriptions", json=payload)
        if response.status_code != 201:
            LOG.error(f'{response.status_code} creating subscription has failed due to {response.text}')
        else:
            self.user_uuid = response.json()['id']
            LOG.info(f'subscription created for id {self.user_uuid}')
        LOG.info('deleting subscription')
        LOG.debug(f'deleting subscription for id: {self.user_uuid}')
        response = self.client.delete(f'/notification/v1/subscriptions/{self.user_uuid}')
        if response.status_code != 204:
            LOG.error(f'{response.status_code}deleting subscription has failed due to {response.text}')

    @events.test_stop.add_listener
    def _(environment, **kw):
        if environment.stats.total.avg_response_time > 3000:
            LOG.error("Test failed due to average response time ratio > 3000 ms")
            environment.process_exit_code = 1
        if environment.stats.total.max_response_time > 3000:
            LOG.error("Test failed due to max response time ratio > 3000 ms")
            environment.process_exit_code = 1
        else:
            environment.process_exit_code = 0


class UsersPool(HttpUser):
    tasks = {NotificationServiceLoadTest}
    wait_time = constant(1)
