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
set -e
set -u
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE USER nsdbuser;
  CREATE DATABASE nsdb;
  GRANT ALL PRIVILEGES ON DATABASE nsdb TO nsdbuser;
EOSQL



