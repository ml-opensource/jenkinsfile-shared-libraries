/**
 * Our android projects don't run the full suite of tests by default (that would
 * really slow down development), so this script enables all of that.
 * <ol>
 *     Here's a breakdown of what this method does:
 *     <li>Download the <code>qualityFile</code> from Jenkins</li>
 *     <li>Call {@link #append} to make <code>buildGradleFile</code> apply <code>qualityFile</code></li>
 *     <li>Download the <code>buildscript</code> from Jenkins</li>
 *     <li>Append the contents of the buildscript file to the top-level build.gradle file</li>
 * </ol>
 * <p>
 *     This <em>is</em> technically optional, but please try to integrate the
 *     structure at earliest convenience. Leave an issue on the repository or
 *     the JIRA board if you want to use a new quality file.
 * </p>
 *
 * @param qualityFile     a gradle-compatible script file, hosted on Jenkins, which can check
 *     code quality
 * @param buildscript     a gradle-compatible script file, hosted on Jenkins, which has extra
 *     maven repos and classpath entries for running those checks
 * @param buildgradlefile a build.gradle file; this will be adjusted so that it applies
 *     <code>qualityfile</code> during the configuration phase
 * @return nothing
 */
def call(String qualityFile = "quality.gradle", String buildscript = "buildscript.gradle", String buildGradleFile = "app/build.gradle") {
    try {
        sh "curl -O '${env.HUDSON_URL}/userContent/jenkins-scripts/android/${qualityFile}'"
        append("../${qualityFile}", buildGradleFile)
        sh "curl ${env.HUDSON_URL}/userContent/jenkins-scripts/android/${buildscript} >> build.gradle"
    } catch (Throwable t) {
        println "Reporting will fail due to missing files" 
    }
}

/**
 * Apply the Plugin represented by <code>qualityFile</code> to <code>buildGradleFile</code>.
 * <p>
 *     This is automatically called by {@link #call the main command}, right after that method
 *     downloads <code>qualityFile</code>. Scripts are free to invoke this multiple times with
 *     different file paths.
 * </p>
 *
 * @param pathToQualityFile a gradle-compatible script file, hosted on Jenkins, which can check
 *     code quality
 * @param buildgradlefile   a build.gradle file; this will be adjusted so that it applies
 *     <code>qualityfile</code> during the configuration phase
 * @return nothing
 */
def append(String pathToQualityFile = "../quality.gradle", String buildGradleFile) {
    sh "printf \"\n\" >> ${buildGradleFile}"
    sh "echo \"apply from: '${pathToQualityFile}'\" >> ${buildGradleFile}"
}
