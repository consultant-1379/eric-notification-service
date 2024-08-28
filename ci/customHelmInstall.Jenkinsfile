#!/usr/bin/env groovy

def bob = "./bob/bob"
def ruleset = "ci/local_ruleset.yaml"

try {
        stage('Custom Helm-Install') {
                sh "${bob} -r ${ruleset} helm-install"
        }
} catch (e) {
    throw e
} finally {
    echo "DONE"
}