#!/usr/bin/env groovy

def bob = new BobCommand()
              .envVars([
              ADP_PORTAL_API_KEY: '${ADP_PORTAL_API_KEY}',
              SELI_ARTIFACTORY_REPO_USER: '${CREDENTIALS_SELI_ARTIFACTORY_USR}',
              SELI_ARTIFACTORY_REPO_PASS: '${CREDENTIALS_SELI_ARTIFACTORY_PSW}'])
              .toString()

pipeline {
    agent {
        node {
            label "${params.NODE_LABEL}"
        }
    }

    environment {
        RELEASE = "false"
        TEAM_NAME = "Photon Team"
        KUBECONFIG = "$HOME/.kube/config"
        DOCKER_CREDS = credentials('armdocker-so-login')
        ARTIFACTORY_CREDS = credentials('artifactory-esoadm-login')
        HELM_CREDS = credentials('armhelm.so.login')
        DOCKER_CONFIG_JSON_FILE_NAME = 'armdockerconfig'
        REPORT_FILE='tests/performance/results_stats.csv'
        CREDENTIALS_SELI_ARTIFACTORY = credentials('esoadm100-seli-artifactory')
    }

    stages {
        stage('Clean') {
            steps {
                configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}/.m2/")]) {
                configFileProvider([configFile(fileId: "${env.KUBECONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}/.kube/")]) { }
                configFileProvider([configFile(fileId: "${env.DOCKER_CONFIG_JSON_FILE_NAME}", targetLocation: "${env.WORKSPACE}/.docker/config.json")]) { }
                }
                sh "git config user.email 'lciadm100@ericsson.com'"
                sh "git config user.name 'lciadm100'"
                sh "${bob} clean"
                echo "Is this job of kind IS_BAGE_IMAGE_UPDATE ? ${params.IS_BASE_IMAGE_UPDATE}"
            }
        }

        stage('Init') {
            steps {
                script {
                    if ( params.IS_BASE_IMAGE_UPDATE ) {
                        sh "${bob} init-drop-base-image-update"
                        archiveArtifacts 'artifact.properties'
                    } else {
                        sh "${bob} init-drop"
                        archiveArtifacts 'artifact.properties'
                    }
                }
            }
        }

//        stage('[MUNIN] Munin Update') {
//            steps {
//                withCredentials([string(credentialsId: 'munin_token', variable: 'MUNIN_TOKEN')]) {
//                    sh "${bob} munin-update-version"
//                }
//            }
//        }

        stage('Package Helm Chart') {
            steps {
                sh "${bob} package"
            }
        }

        stage('Lint') {
            steps {
                parallel(
                        "lint markdown": {
                            sh "${bob} lint:markdownlint lint:vale"
                        },
                        "lint helm": {
                            sh "${bob} lint:helm"
                        },
                        "lint helm design rule checker": {
                            sh "${bob} lint:helm-chart-check"
                        },
                        "lint dry run install": {
                            sh "${bob} lint:helm-dry-run-install"
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'helm-install-dry-run.log'
                        }
                )
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '.bob/design-rule-check-report.html'
                }
            }
        }

        stage('Build Source Code') {
            steps {
                sh "${bob} build"
            }
        }

        stage('3pp Analysis') {
            steps {
                sh "${bob} 3pp"
            }
        }

        stage('Maven Tests') {
            steps {
                sh "${bob} maven-test"
            }
        }

        stage('SonarQube Analysis') {
            steps {
                sh "${bob} sonar"
            }
        }

        // fetch latest OS and java11 versions and fix dockerfile
        stage('Update to latest Base Image') {
            when {
                expression { params.IS_BASE_IMAGE_UPDATE }
            }
            steps {
                sh "${bob} common-base-update"
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "${bob} apply-versions"
                sh "${bob} image"
            }
        }

        stage('Functional Test') {
            steps {
                sh "${bob} start-test-env"
                sh "${bob} functional-test"
            }
        }
        stage('Integration Tests') {
            steps {
                sh "${bob} integration-test"
            }
        }
        stage('Performance Test'){
            steps{
                sh "${bob} load-test"
                sh "${bob} stop-test-env"
            }
        }
        stage('Plot Backup Size and Duration Graphs') {
            steps {
                script {
                    plot csvFileName: 'results_stats.csv',
                            csvSeries: [[file            : env.REPORT_FILE,
                                         exclusionValues : 'Average Content Size,Requests/s,Failures/s,50%,66%,75%,80%,90%,95%,99%,99.90%,99.99%,100%',
                                         displayTableFlag: false,
                                         inclusionFlag   : 'OFF',
                                         url             : '']],
                            group: 'Performance tests',
                            title: 'Performance test',
                            yaxis: 'Request Count',
                            style: 'bar',
                            numBuilds: '100'
                }
            }
        }

        stage('Commit latest Base Image') {
            when {
                expression { params.IS_BASE_IMAGE_UPDATE }
            }
            steps {
                sh "${bob} commit-tag-upload"
            }
        }

        stage('Publish') {
            steps {
                sh "${bob} publish"
            }
        }

        stage('Build Docs') {
            steps {
                sh "${bob} generate-docs"
                archiveArtifacts 'build/doc-marketplace/*.zip'
            }
        }

        stage('Publish Docs') {
            steps {
                sh "${bob} publish-docs"
            }
        }

        stage('SDK Validation') {
            steps {
                sh "${bob} validate-sdk"
            }
        }

        stage('Publish SDK Docs') {
            steps {
               withCredentials([usernamePassword(credentialsId: 'esoadm100-seli-artifactory', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS') ]) {
                sh "${bob} publish-md-oas"
                }
            }
        }
    }
    post {
        always {
            sh "${bob} stop-test-env"
            archiveArtifacts allowEmptyArchive: true, artifacts: ".bob/3pp-results.html"
            archiveArtifacts allowEmptyArchive: true, artifacts: ".bob/3pp-consistency.csv"
        }
    }
}

def getQualityGate() {
    // Wait for SonarQube Analysis is done and Quality Gate is pushed back
    qualityGate = waitForQualityGate()

    // If Analysis file exists, parse the Dashboard URL
    if (fileExists(file: 'target/sonar/report-task.txt')) {
        sh 'cat target/sonar/report-task.txt'
        def props = readProperties file: 'target/sonar/report-task.txt'
        env.DASHBOARD_URL = props['dashboardUrl']
    }

    if (qualityGate.status != 'OK') { // If Quality Gate Failed
        if (env.GERRIT_CHANGE_NUMBER) {
            env.SQ_MESSAGE = "'" + "SonarQube Quality Gate Failed: ${DASHBOARD_URL}" + "'"
            sh '''
               ssh -p 29418 lciadm100@gerrit.ericsson.se gerrit review --label 'SQ-Quality-Gate=-1'  \
                 --message ${SQ_MESSAGE} --project $GERRIT_PROJECT $GERRIT_PATCHSET_REVISION
            '''
            error "Pipeline aborted due to quality gate failure!\n Report: ${env.DASHBOARD_URL}"
        }
    } else if (env.GERRIT_CHANGE_NUMBER) { // If Quality Gate Passed
        env.SQ_MESSAGE = "'" + "SonarQube Quality Gate Passed: ${DASHBOARD_URL}" + "'"
        sh '''
            ssh -p 29418 lciadm100@gerrit.ericsson.se gerrit review --label 'SQ-Quality-Gate=+1'  \
                --message ${SQ_MESSAGE} --project $GERRIT_PROJECT $GERRIT_PATCHSET_REVISION
         '''
    }
}

// More about @Builder: http://mrhaki.blogspot.com/2014/05/groovy-goodness-use-builder-ast.html
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class BobCommand {
    def bobImage = 'armdocker.rnd.ericsson.se/sandbox/adp-staging/adp-cicd/bob.2.0:1.7.0-20'
    def envVars = [:]
    def additionalVolumes = []
    def needDockerSocket = true

    // executes bob with --quiet parameter to reduce noise. Set to false if you need to get more details.
    def quiet = true

    String toString() {
        def env = envVars
                .collect({ entry -> "-e ${entry.key}=\"${entry.value}\"" })
                .join(' ')
        def volumes = additionalVolumes
                .collect({ line -> "-v \"${line}\"" })
                .join(' ')
        def cmd = """\
            |docker run
            |--init
            |--rm
            |--workdir \${PWD}
            |--user \$(set +x; id -u):\$(set +x; id -g)
            |-v \${PWD}:\${PWD}
            |-v /etc/group:/etc/group:ro
            |-v /etc/passwd:/etc/passwd:ro
            |-v \${PWD}/.docker:\${HOME}/.docker
            |-v \${HOME}/.ssh:\${HOME}/.ssh
            |${needDockerSocket ? '-v /var/run/docker.sock:/var/run/docker.sock' : ''}
            |${env}
            |${volumes}
            |\$(set +x; for group in \$(id -G); do printf ' --group-add %s' "\$group"; done)
            |${bobImage}
            |${quiet ? '--quiet' : ''}
            |"""
        return cmd
                .stripMargin()           // remove indentation
                .replace('\n', ' ')      // join lines
                .replaceAll(/[ ]+/, ' ') // replace multiple spaces by one

    }
}
