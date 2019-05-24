/**
 * Our android projects don't run the full suite of tests by default (that would
 * really slow down development), so this script enables all of that.
 * <ol>
 *     Here's a breakdown of what this method does:
 *     <li>Download the <code>qualityFile</code> from Jenkins</li>
 *     <li>Add an <code>apply from: '../qualityFile'</code> to the application build.gradle file</li>
 *     <li>Download the <code>buildscript</code> from Jenkins</li>
 *     <li>Append the contents of the buildscript file to the top-level build.gradle file</li>
 * </ol>
 * <p>
 *     This <em>is</em> technically optional, but please try to integrate the
 *     structure at earliest convenience. Leave an issue on the repository or
 *     the JIRA board if you want to use a new quality file.
 * </p>
 *
 * @param qualityFile a gradle-compatible script file, hosted on Jenkins, which can check code quality
 * @param buildscript a gradle-compatible script file, hosted on Jenkins, which has extra maven
 * repos and classpath entries for running those checks
 * @return nothing
 */
def call(String qualityFile = "quality_six.gradle", String buildscript = "buildscript.gradle") {
    try {
        sh "curl -O '${env.HUDSON_URL}/userContent/jenkins-scripts/android/${qualityFile}'"
        sh 'printf "\n" >> app/build.gradle'
        sh "echo \"apply from: '../${qualityFile}'\" >> app/build.gradle"
        sh "curl ${env.HUDSON_URL}/userContent/jenkins-scripts/android/${buildscript} >> build.gradle"
    } catch (Throwable t) {
        println "Reporting will fail due to missing files" 
    }
}
