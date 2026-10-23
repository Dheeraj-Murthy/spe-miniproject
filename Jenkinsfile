pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    triggers {
        // Webhook preferred (configure GitHub webhook -> Jenkins /github-webhook/).
        // Poll SCM kept as a fallback for local/offline demo use.
        pollSCM('H/5 * * * *')
    }

    environment {
        // launchd-started Jenkins has a minimal PATH/JAVA_HOME; pin both explicitly
        // so `mvn`/`docker` resolve and compile against the same JDK 22 used locally.
        JAVA_HOME = '/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home'
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        IMAGE_NAME = 'dheerajmurthy/calculator-api'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STUDENT_EMAIL = 'ayushtiwari1405@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Push') {
            steps {
                sh '''
                    echo "$DOCKERHUB_CREDENTIALS_PSW" | docker login -u "$DOCKERHUB_CREDENTIALS_USR" --password-stdin
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${IMAGE_NAME}:latest
                '''
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    export DOCKER_IMAGE=${IMAGE_NAME}:${IMAGE_TAG}
                    docker compose up -d --remove-orphans
                '''
                // Give the container's own HEALTHCHECK a moment, then verify from the host.
                sh '''
                    for i in $(seq 1 10); do
                        if curl -sf http://localhost:8082/actuator/health; then
                            exit 0
                        fi
                        sleep 3
                    done
                    echo "Health check failed after deploy"
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
        success {
            mail to: "${STUDENT_EMAIL}",
                 subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build and deploy succeeded.\n\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nURL: ${env.BUILD_URL}"
        }
        failure {
            mail to: "${STUDENT_EMAIL}",
                 subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed.\n\nJob: ${env.JOB_NAME}\nBuild: ${env.BUILD_NUMBER}\nURL: ${env.BUILD_URL}\n\nCheck console output for details."
        }
    }
}
