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

FROM armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-python3builder

ARG APP_UID=1001
ARG APP_GID=1001
ENV APP_DIR="/home/ns_admin/conftest/"


RUN zypper addrepo -C -G -f https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/2.1.0-22?ssl_verify=no LDC-CBO-SLES \
    && zypper refresh -f -r LDC-CBO-SLES \
    && zypper install -y python3 glibc-locale curl \
    && zypper --non-interactive install shadow \
    && zypper clean --all \
    && rm -rf /var/cache/zypp

RUN groupadd ns_admin --gid ${APP_GID} && \
    useradd ns_admin --uid ${APP_UID} --gid ${APP_GID} --create-home && \
    mkdir -p ${APP_DIR}

#Remove useradd and groupadd cli
RUN zypper --non-interactive remove shadow

#Reduce image size
RUN mv /usr/lib/locale/C.utf8 /C.utf8 \
    && rm -rf /usr/lib/locale/* \
    && mv /C.utf8 /usr/lib/locale/

WORKDIR ${APP_DIR}

USER ns_admin

COPY nbitestsuite/ ${APP_DIR}

ENV PATH "$PATH:/home/ns_admin/.local/bin"
ENV LC_ALL=C.UTF-8
ENV LANG=${LC_ALL}

RUN python3 -m pip install pipenv

RUN python3 -m pipenv install --deploy --system