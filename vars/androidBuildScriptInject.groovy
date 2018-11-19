def call(String qualityFile = "quality_six.gradle", String buildscript = "buildscript.gradle") {
    sh "curl -O 'https://jenkins.fuzzhq.com/userContent/jenkins-scripts/android/${qualityFile}'"
    sh 'printf "\n" >> app/build.gradle'
    sh "echo \"apply from: '../${qualityFile}'\" >> app/build.gradle"
    sh "curl https://jenkins.fuzzhq.com/userContent/jenkins-scripts/android/${buildscript} >> build.gradle"
}