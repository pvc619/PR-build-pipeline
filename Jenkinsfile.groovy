pipeline {
    agent any
    
    environment {
        EMAIL_RECIPIENTS = 'your.email@example.com' // Replace with your email
        timestamps() // Add this line to enable timestamps in the console output
    }

    options {
        skipDefaultCheckout(true) // Skip default checkout to perform custom checkout later
    }

    triggers {
        githubPullRequest {
            useGitHubHooks()
        }


    stages {
        stage('Checkout') {
            steps {
                script {
                    try {
                        // Custom checkout of the repository with additional branch configuration
                        checkout([$class: 'GitSCM', 
                                  branches: [[name: '${sha1}']],
                                  doGenerateSubmoduleConfigurations: false,
                                  extensions: [
                                      [$class: 'CloneOption', noTags: false, shallow: false, depth: 0, reference: ''],
                                      [$class: 'CleanBeforeCheckout']
                                  ],
                                  userRemoteConfigs: [[
                                      url: 'https://github.com/pvc619/PR-build-pipeline.git', 
                                      credentialsId: 'test-pipeline-credentials',
                                      refspec: '+refs/pull/*:refs/remotes/origin/pr/*'
                                  ]],
                                  browser: [$class: 'GithubWeb', url: 'https://github.com/pvc619/PR-build-pipeline/blob/PR-Branch/src/main/java/com/example/App.java']
                        ])
                    } catch (Exception e) {
                        error("Checkout Failed: ${e.message}")
                    }
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    try {
                        // Define the path to Maven
                        def mavenHome = '/opt/maven'
                        def mavenCommand = "${mavenHome}/bin/mvn"
                        
                        // Execute Maven build with clean package goals
                        sh "${mavenCommand} -f /PR-build-pipeline/pom.xml clean package"
                    } catch (Exception e) {
                        error("Build Failed: ${e.message}")
                    }
                }
            }
        }

        
    }

    post {
        always {
            script {
                def commitStatus = currentBuild.result == 'SUCCESS' ? 'success' : 'failure'
                def message = currentBuild.result == 'SUCCESS' ? 'Build succeeded!' : 'Build failed!'
                githubNotify context: 'jenkins/build', status: commitStatus, description: message
            }
        }
        success {
            echo 'Pull Request build successful!'
        }
        failure {
            echo 'Pull Request build failed!'
        }
    }
}
}
