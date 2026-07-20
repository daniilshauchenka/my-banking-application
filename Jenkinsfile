def services = [
    'api-gateway',
    'account-service',
    'transfer-service',
    'cash-service',
    'notification-service',
    'front-ui'
]

def runCommand(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat command
    }
}

def runMaven(String service, String goals) {
    dir(service) {
        if (isUnix()) {
            sh "./mvnw ${goals}"
        } else {
            bat "mvnw.cmd ${goals}"
        }
    }
}

pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_K8S', defaultValue: false, description: 'Deploy the Helm chart to the current Kubernetes context.')
        string(name: 'IMAGE_PREFIX', defaultValue: 'my-bank', description: 'Docker image repository prefix.')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'Docker image tag.')
        string(name: 'K8S_NAMESPACE', defaultValue: 'my-bank', description: 'Kubernetes namespace.')
    }

    environment {
        HELM_CHART = 'helm/my-bank'
        HELM_RELEASE = 'my-bank'
    }

    stages {
        stage('test services') {
            steps {
                script {
                    services.each { service ->
                        runMaven(service, 'test')
                    }
                }
            }
        }

        stage('build images') {
            steps {
                script {
                    services.each { service ->
                        runCommand("docker build -t ${params.IMAGE_PREFIX}/${service}:${params.IMAGE_TAG} ${service}")
                    }
                }
            }
        }

        stage('validate helm') {
            steps {
                runCommand("helm lint ${env.HELM_CHART}")
                runCommand("helm template ${env.HELM_RELEASE} ${env.HELM_CHART} --set global.image.repositoryPrefix=${params.IMAGE_PREFIX} --set global.image.tag=${params.IMAGE_TAG}")
            }
        }

        stage('deploy') {
            when {
                expression { return params.DEPLOY_TO_K8S }
            }
            steps {
                runCommand("helm upgrade --install ${env.HELM_RELEASE} ${env.HELM_CHART} --namespace ${params.K8S_NAMESPACE} --create-namespace --set global.image.repositoryPrefix=${params.IMAGE_PREFIX} --set global.image.tag=${params.IMAGE_TAG}")
            }
        }

        stage('helm test') {
            when {
                expression { return params.DEPLOY_TO_K8S }
            }
            steps {
                runCommand("helm test ${env.HELM_RELEASE} --namespace ${params.K8S_NAMESPACE} --timeout 120s")
            }
        }
    }
}
