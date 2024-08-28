#
# COPYRIGHT Ericsson 2020
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

# OS_BASE_VERSION and JAVA11_VERSION is set from bob

FROM armdocker.rnd.ericsson.se/proj-ldc/common_base_os/sles:OS_BASE_VERSION
RUN zypper ar -C -G -f https://arm.sero.gic.ericsson.se/artifactory/proj-ldc-repo-rpm-local/common_base_os/sles/OS_BASE_VERSION?ssl_verify=no LDC-CBO-SLES \
    && zypper ref -f -r LDC-CBO-SLES \
    && zypper --non-interactive in java-11-openjdk-JAVA11_VERSION \
    && zypper --non-interactive in curl iputils openssh \
    && zypper clean -a \
    && rm -rf /var/cache/zypp \
    && zypper -n remove zypper

ARG JAR_FILE
ARG COMMIT
ARG BUILD_DATE
ARG APP_VERSION
LABEL \
    org.opencontainers.image.title=eric-oss-notification-service-jsb \
    org.opencontainers.image.created=$BUILD_DATE \
    org.opencontainers.image.revision=$COMMIT \
    org.opencontainers.image.vendor=Ericsson \
    org.opencontainers.image.version=$APP_VERSION \
    com.ericsson.product-number="CXU1010710" \
    com.ericsson.product-revision="R1A"

ARG USER_ID=256399
ARG USER_NAME="eric-oss-notification-service"

ADD target/${JAR_FILE} notification-service.jar
COPY src/main/resources/jmx/* /jmx/
RUN chmod 600 /jmx/jmxremote.password
RUN chown $USER_ID /jmx/jmxremote.password

RUN mkdir -p /tmpDir \
    && chown -R $USER_ID /tmpDir \
    && chmod -R 755 /tmpDir

RUN echo "$USER_ID:x:$USER_ID:0:An Identity for $USER_NAME:/nonexistent:/bin/false" >>/etc/passwd
RUN echo "$USER_ID:!::0:::::" >>/etc/shadow

USER $USER_ID
EXPOSE 8080
CMD java ${JAVA_OPTS} -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1099 -Djava.io.tmpdir=/tmpDir \
-Dcom.sun.management.jmxremote.authenticate=true -Dcom.sun.management.jmxremote.ssl=false \
-Dcom.sun.management.jmxremote.rmi.port=1099 -Dcom.sun.management.jmxremote.password.file=/jmx/jmxremote.password \
-Dcom.sun.management.jmxremote.access.file=/jmx/jmxremote.access -jar notification-service.jar
