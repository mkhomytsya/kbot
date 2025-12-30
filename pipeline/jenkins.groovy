pipeline {
    agent any

    parameters {
        choice(name: 'TARGETOS', choices: ['linux', 'darwin', 'windows'], description: 'Target OS')
        choice(name: 'TARGETARCH', choices: ['amd64', 'arm64'], description: 'Target Architecture')
        string(name: 'VERSION', defaultValue: 'v1.0.0', description: 'Version tag')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run tests')
        booleanParam(name: 'BUILD_IMAGE', defaultValue: true, description: 'Build and push Docker image')
    }

    environment {
        APP = 'kbot'
        REGISTRY = 'ghcr.io/mkhomytsya'
        GITHUB_REPO = 'mkhomytsya/kbot'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/${GITHUB_REPO}.git'
            }
        }

        stage('Setup Go') {
            steps {
                sh 'go version'
                sh 'go mod tidy'
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS }
            }
            steps {
                sh 'make test'
            }
        }

        stage('Build') {
            steps {
                sh "TARGETOS=${params.TARGETOS} TARGETARCH=${params.TARGETARCH} make build"
            }
        }

        stage('Build and Push Image') {
            when {
                expression { params.BUILD_IMAGE }
            }
            steps {
                script {
                    def version = "${params.VERSION}-${sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()}"
                    env.VERSION = version
                }
                sh "VERSION=${env.VERSION} TARGETOS=${params.TARGETOS} TARGETARCH=${params.TARGETARCH} make image"
                withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh "echo ${PASSWORD} | docker login ghcr.io -u ${USERNAME} --password-stdin"
                }
                sh "VERSION=${env.VERSION} TARGETOS=${params.TARGETOS} TARGETARCH=${params.TARGETARCH} make push"
            }
        }

        stage('Update Helm Chart') {
            when {
                expression { params.BUILD_IMAGE }
            }
            steps {
                sh "yq -i '.image.tag=\"${env.VERSION}\"' helm/values.yaml"
                sh "yq -i '.image.arch=\"${params.TARGETARCH}\"' helm/values.yaml"
                sh 'git config user.name "jenkins"'
                sh 'git config user.email "jenkins@example.com"'
                sh 'git add helm/values.yaml'
                sh "git commit -m 'chore: update version to ${env.VERSION}' || echo 'No changes'"
                withCredentials([usernamePassword(credentialsId: 'github-token', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh "git push https://${USERNAME}:${PASSWORD}@github.com/${GITHUB_REPO}.git main"
                }
            }
        }
    }

    post {
        always {
            sh 'make clean'
        }
    }
}