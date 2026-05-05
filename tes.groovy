pipeline {
    agent any

    environment {
        // Ganti dengan username Docker Hub yang sesuai
        DOCKER_HUB_USER = 'viviags' 
        // ID kredensial yang dibuat di 'Add Credentials' tadi
        DOCKER_HUB_ID   = 'dockerhub-login'
        // URL repo GitHub aplikasi
        GIT_REPO_URL    = 'https://github.com/viviags/kantin-app.git'
    }

    stages {
        stage('Checkout Code') {
            steps {
                // Tahap mengambil kode terbaru dari repositori, pastikan branch sesuai yang digunakan
                git branch: 'main', url: "${GIT_REPO_URL}"
            }
        }

        stage('Build Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_ID}", 
                                passwordVariable: 'PASS', 
                                usernameVariable: 'USER')]) {
                    bat 'echo %PASS%| docker login -u %USER% --password-stdin'
                    
                    bat "docker build -t %DOCKER_HUB_USER%/kantin-backend:latest ./backend"
                    bat "docker build -t %DOCKER_HUB_USER%/kantin-frontend:latest ./frontend"
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_ID}", 
                                passwordVariable: 'PASS', 
                                usernameVariable: 'USER')]) {
                    bat "echo %PASS%| docker login -u %USER% --password-stdin"
                    bat "docker push %DOCKER_HUB_USER%/kantin-backend:latest"
                    bat "docker push %DOCKER_HUB_USER%/kantin-frontend:latest"
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                echo 'Deploying to Kubernetes using Secret File...'
                // Jenkins akan mengambil file 'kube-config' dan menaruhnya di folder temporary
                withCredentials([file(credentialsId: 'kube-config', variable: 'KUBECONFIG_FILE')]) {
                    // Kita panggil kubectl dengan bendera --kubeconfig yang mengarah ke file temp tadi
                    bat 'kubectl --kubeconfig=%KUBECONFIG_FILE% apply -f kantin-k8s.yaml --validate=false'
                    bat 'kubectl --kubeconfig=%KUBECONFIG_FILE% apply -f kantin-ingress.yaml --validate=false'
                }
            }
        }
    }
}