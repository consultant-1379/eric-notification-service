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

import requests
import logging
import json
import uuid
from base64 import b64encode
from os import urandom
from ..constants import keys
from ..tests import creation_test
from pytest_bdd import scenario, given, when, then

LOG = logging.getLogger(__name__)


@scenario("deletion.feature", "delete request OK")
def test_deletion_ok():
    pass


@given("create new subscription", target_fixture="subscription")
def create_new_subscription_bg(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com/" + b64encode(urandom(16)).decode('utf-8'),
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent",
                "filterCriteria": "event.priority=gt=1;event.priority=lt=10;(tenant==tenant1,tenant==tenant)",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return creation_test.send_creation_request(ns_server_params, json.loads(subscription))


@given("creation response code should be 201")
def check_creation_response_code(subscription):
    LOG.debug("Subscription response %s.", subscription.text.encode("utf8"))
    assert subscription.status_code == 201


@given("send deletion request ok", target_fixture="delete_response_ok")
def send_deletion_request(ns_server_params, subscription):
    LOG.debug("Subscription response id=%s.", subscription.json()['id'])
    return send_delete_request(ns_server_params, subscription.json()['id'])


@then("delete response code should be 204")
def check_delete_response_status_ok(delete_response_ok):
    assert delete_response_ok.status_code == 204


##########################################################
@scenario("deletion.feature", "delete request NOT_FOUND")
def test_deletion_not_found():
    pass


@given("send deletion request not found", target_fixture="delete_response_not_found")
def send_deletion_request_not_found(ns_server_params):
    return send_delete_request(ns_server_params, str(uuid.uuid4()))


@then("delete response code should be 404")
def check_delete_response_status_not_found(delete_response_not_found):
    assert delete_response_not_found.status_code == 404


##########################################################
@scenario("deletion.feature", "delete request UUID validation")
def test_deletion_wrong_uuid():
    pass


@given("send deletion request wrong uuid", target_fixture="delete_response_wrong_uuid")
def send_deletion_request_wrong_uuid(ns_server_params):
    wrong_uuid = '123'
    return dict(uuid=wrong_uuid, response=send_delete_request(ns_server_params, wrong_uuid))


@then("delete response code should be 400")
def check_delete_response_status_wrong_uuid(delete_response_wrong_uuid):
    assert delete_response_wrong_uuid['response'].status_code == 400


@then("check delete response payload")
def check_delete_response_wrong_uuid_payload(delete_response_wrong_uuid):
    assert 'errorCode' in delete_response_wrong_uuid['response'].json()
    assert delete_response_wrong_uuid['response'].json()['errorCode'] == "ENS-B-12"


##########################################################
def send_delete_request(ns_server_param, subscription_uuid):
    ns_url = ns_server_param[keys.BASE_URL] + "/notification/v1/subscriptions/" + subscription_uuid
    LOG.debug("Send deletion request to %s. UUID=%s", ns_url, subscription_uuid)
    response = requests.delete(ns_url, headers=ns_server_param[keys.REQUEST_HEADERS])
    LOG.debug("Send deletion request. response: %s", response.text.encode("utf8"))
    return response
