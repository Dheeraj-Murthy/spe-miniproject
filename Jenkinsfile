pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    triggers {
        // GitHub webhook -> Jenkins /github-webhook/ (requires GitHub plugin + a
        // publicly reachable Jenkins URL, e.g. via ngrok for local demo use).
        githubPush()
    }

    environment {
        // launchd-started Jenkins has a minimal PATH/JAVA_HOME; pin both explicitly
        // so `mvn`/`docker` resolve and compile against the same JDK 22 used locally.
        JAVA_HOME = '/Library/Java/JavaVirtualMachines/jdk-22.jdk/Contents/Home'
        PATH = "${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        IMAGE_NAME = 'dheerajmurthy/calculator-api'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STUDENT_EMAIL = 'ms.dheerajmurthy@iiitb.ac.in'
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
                // calculator-api has no published host port (scaled to 2 replicas behind
                // nginx), so verify through the proxy instead of hitting it directly.
                sh '''
                    for i in $(seq 1 10); do
                        if curl -sf http://localhost:8081/actuator/health; then
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
