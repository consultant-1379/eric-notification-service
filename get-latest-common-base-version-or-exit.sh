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

############################################################################
# Script to automatically change the common base image to the latest version
# exit code (0) if OK (1) if the latest version is same the current
############################################################################
#!/bin/bash
echo "Querying for the latest OS base version..."
common_base_version=$(curl -u esoadm100:AKCp5dKspy6qdk8sBzXNwPn4UWrJNmNbG6VzwZTZpT2vCCwcRetKj7fuzpAa6RYczigYrfjVv \
-X POST https://arm.epk.ericsson.se/artifactory/api/search/aql \
-H "content-type: text/plain" \
-d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-ldc/common_base_os_release/sles/*-*"}}).sort({"$desc": ["created"]}).limit(1)' \
2>/dev/null | grep path | sed -e 's_.*\/\(.*\)".*_\1_')

[ -z "$common_base_version" ] && echo "could not find OS base version" && exit 1
echo "$common_base_version" > .bob/var.latest-os-base-version

echo -n "Latest OS base version:  "
cat ./.bob/var.latest-os-base-version

echo -n "Current OS base version: "
cat ./.bob/var.current-os-base-version

if [[ "$(cat ./.bob/var.current-os-base-version)" == "$(cat ./.bob/var.latest-os-base-version)" ]]; then
    echo "Latest OS common base is already employed - exiting"
    exit 1
fi
echo "Latest OS common base will be employed"
exit 0
