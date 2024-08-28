#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

@Library('oss-common-pipeline-lib@dVersion-2.0.0-hybrid') _   // Shared library from the OSS/com.ericsson.oss.ci/oss-common-ci-utils

try {
        if (env.RELEASE) {
            stage('Custom Publish') {
                withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
                                                file(credentialsId: 'docker-config-json', variable: 'DOCKER_CONFIG_JSON')]) {
                    ci_pipeline_scripts.checkDockerConfig()
                    ci_pipeline_scripts.retryMechanism("${bob} -r ${ruleset} publish",3)
                }
            }
        }
} catch (e) {
    throw e
} finally {
    echo "DONE"
}