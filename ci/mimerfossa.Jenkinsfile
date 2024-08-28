#!/usr/bin/env groovy
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def bob = './bob/bob'
def bob_mimer = './bob/bob -r ci/mimer_ruleset.yaml'

try {
    stage('FOSS Validation for Mimer') {
        if (env.MUNIN_UPDATE_ENABLED == "true" ){
            withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
                sh "${bob_mimer} munin-update-version"
            }
        }
	}
} catch (e) {
    throw e
} finally {
    echo "**** (Munin Update) Finally done **** "
}