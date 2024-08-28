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

[ -z "$1" ] && echo "$0 OS_BASE_VERSION" && exit 1
OS_BASE_VERSION=$1
echo "pre $OS_BASE_VERSION"
curl -v -s  -u esoadm100:AKCp5dKspy6qdk8sBzXNwPn4UWrJNmNbG6VzwZTZpT2vCCwcRetKj7fuzpAa6RYczigYrfjVv https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/${OS_BASE_VERSION}/x86_64?ssl_verify=no -o rpms.html
echo "post"
echo "$java_11_version" > .bob/var.java_11_version
exit 0
