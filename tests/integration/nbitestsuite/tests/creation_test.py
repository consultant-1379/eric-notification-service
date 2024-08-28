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
from base64 import b64encode
from os import urandom
from ..constants import keys
from pytest_bdd import scenario, given, when, then

LOG = logging.getLogger(__name__)


@scenario("creation.feature", "request OK")
def test_creation_ok():
    pass


@given("valid creation request was sent", target_fixture="creation_response")
def send_valid_request(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com",
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent",
                "filterCriteria": "event.priority=gt=1;event.priority=lt=10;(tenant==tenant1,tenant==tenant)",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response code should be 201")
def check_response_code_is_201(creation_response):
    assert creation_response.status_code == 201


@then("response body contains subscription params from a request plus UUID")
def check_response_body_contains_id(creation_response):
    created_subscription = creation_response.json()
    assert 'id' in created_subscription
    assert created_subscription['id']

##########################################################


@scenario("creation.feature", "request BAD REQUEST (filterCriteria)")
def test_creation_invalid_filter_criteria_bad_request():
    pass


@given("invalid filterCriteria in creation request was sent", target_fixture="creation_response")
def send_invalid_filter_criteria_request(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com",
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent",
                "filterCriteria": "tenant=tenant1,tenant=tenant",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response code should be 400")
def check_response_code_is_400(creation_response):
    assert creation_response.status_code == 400


@then("response body contains a filterCriteria error details")
def check_response_body_contains_filter_criteria_error_details(creation_response):
    created_subscription = creation_response.json()
    assert 'errorCode' in created_subscription
    assert 'userMessage' in created_subscription
    assert created_subscription['errorCode'] == "ENS-C-02"
    assert created_subscription['userMessage'] == "Error creating subscription. The request has wrong parameters: field \'filterCriteria\' has invalid RSQL format. filterCriteria=tenant=tenant1,tenant=tenant"

##########################################################


@scenario("creation.feature", "request BAD REQUEST (address)")
def test_creation_invalid_address_bad_request():
    pass


@given("invalid address in creation request was sent", target_fixture="creation_response")
def send_invalid_address_request(ns_server_params):
    subscription = json.dumps({
        "address": "invalid url",
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent_",
                "filterCriteria": "tenant==tenant1,tenant==tenant",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response body contains an address error details")
def check_response_body_contains_address_error_details(creation_response):
    created_subscription = creation_response.json()
    assert 'errorCode' in created_subscription
    assert 'userMessage' in created_subscription
    assert created_subscription['errorCode'] == "ENS-C-02"
    assert created_subscription['userMessage'] == "Error creating subscription. The request has wrong parameters: Field \'address\' must to be a valid URL"


##########################################################

@scenario("creation.feature", "request BAD REQUEST (eventType)")
def test_creation_no_event_type_bad_request():
    pass


@given("no eventType in creation request was sent", target_fixture="creation_response")
def send_no_event_type_request(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com",
        "subscriptionFilter": [
            {
                "filterCriteria": "tenant==tenant1,tenant==tenant",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response body contains an eventType error details")
def check_response_body_contains_no_event_type_error_details(creation_response):
    created_subscription = creation_response.json()
    assert 'errorCode' in created_subscription
    assert 'userMessage' in created_subscription
    assert created_subscription['errorCode'] == "ENS-B-00"
    assert created_subscription['userMessage'] == "Error creating subscription. The request has missing mandatory fields: Field subscriptionFilter[0].eventType must not be null"

##########################################################

@scenario("creation.feature", "request BAD REQUEST (fields)")
def test_creation_invalid_fields_bad_request():
    pass


@given("invalid fields in creation request was sent", target_fixture="creation_response")
def send_invalid_fields_request(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com",
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent",
                "filterCriteria": "tenant==tenant1,tenant==tenant",
                "fields": ",event,,"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response body contains a fields error details")
def check_response_body_contains_error_details(creation_response):
    created_subscription = creation_response.json()
    assert 'errorCode' in created_subscription
    assert 'userMessage' in created_subscription
    assert created_subscription['errorCode'] == "ENS-C-02"
    assert created_subscription['userMessage'] == "Error creating subscription. The request has wrong parameters: field 'fields' contains invalid json keys. fields=,event,,"


##########################################################


@scenario("creation.feature", "request CONFLICT")
def test_creation_conflict():
    pass


@given("specific creation request was sent", target_fixture="creation_response")
def send_specific_request(ns_server_params):
    subscription = json.dumps({
        "address": "http://integration.test.com",
        "subscriptionFilter": [
            {
                "eventType": "ServiceOrderCreateEvent",
                "filterCriteria": "event.priority=gt=1;event.priority=lt=10;(tenant==tenant1,tenant==tenant)",
                "fields": "event.type,event.priority"
            }
        ], "tenant": "master"
    })
    return send_creation_request(ns_server_params, json.loads(subscription))


@then("response code should be 409")
def check_response_code_is_409(creation_response):
    assert creation_response.status_code == 409


##########################################################
def send_creation_request(ns_server_param, subscription):
    ns_url = ns_server_param[keys.BASE_URL] + "/notification/v1/subscriptions"
    LOG.debug("Send creation request to %s. Payload: %s", ns_url, subscription)
    response = requests.post(ns_url, headers=ns_server_param[keys.REQUEST_HEADERS], json=subscription)
    LOG.debug("Send creation request. response: %s", response.text.encode("utf8"))
    return response
