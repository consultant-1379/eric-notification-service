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
from pytest import fixture
from .constants import keys

LOG = logging.getLogger(__name__)


def pytest_addoption(parser):
    parser.addoption(
        "--url",
        action="store",
        dest="url",
        required=True,
        help="URL address where NS deployed. Testsuite will run against this address")


@fixture
def ns_server_params(request):
    return {
        keys.BASE_URL: request.config.option.url,
        keys.REQUEST_HEADERS: {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
    }