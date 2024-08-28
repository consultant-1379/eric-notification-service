#!/usr/bin/env groovy

def bob = "./bob/bob -r ci/local_ruleset.yaml"

try {
        stage('Custom Maven-Tests') {
				sh "${bob} maven-test"
				sh "${bob} common-base-update"
                sh "${bob} apply-versions"
        }
} catch (e) {
    throw e
} finally {
    echo "**** Finally done **** "
}