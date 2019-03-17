/**
 * <h1>Run tests from repository A over artifacts from build B.</h1>
 * <p>
 *     <h2>If gitRepo is left empty (its default value), this method</h2>
 *     <ol>
 *         <li>ignores gitRepo, branch, and artifactName parameters</li>
 *         <li>runs <code>body()</code></li>
 *         <li>{@link uatStage#reportExtents reports on extents}</li>
 *     </ol>
 *     within a dedicated Stage. The current repository is A and
 *     B is undefined. This is a good choice if the artifact you
 *     wish to test is not (yet) associated with a Jenkins Job.
 * </p>
 * <p>
 *     <h2>If gitRepo is given the name of a repository, this method</h2>
 *     <ol>
 *         <li>{@link git#clone clones} that repository</li>
 *         <li>checks out <code>branch</code> (if it exists)</li>
 *         <li>copies an artifact from build B into the newly-cloned folder</li>
 *         <li>runs <code>body()</code> within the context of that folder</li>
 *         <li>{@link uatStage#reportExtents reports on extents}</li>
 *     </ol>
 *     within a dedicated Stage. The gitRepo repository is A, and
 *     the current build is B. This is the form of execution that
 *     non-automation Jenkinsfiles should use.
 * </p>
 * <p>
 *     <h2>UAT Best Practices</h2>
 *     Try to limit this testing phase to black box tests. More
 *     invasive tests should be run with {@link testStage#call} or
 *     directly through e.g. {@link bash#call}.
 * </p>
 *
 * @param gitRepo      url for repository A
 * @param branch       branch in repository A to use
 * @param artifactName name of an artifact on build B to test
 * @param body         arbitrary code to run right before {@link uatStage#reportExtents}
 * @return nothing
 */
def call(String gitRepo = "", String branch = "", String artifactName = "app.zip", Closure body) {
	stage("UAT") {
		folderName = ""
		folderPath = ""
		if (gitRepo != "") {
			gitURL = gitRepo
			if (!gitRepo.contains("https://") && !gitRepo.contains("git@")) {
				gitURL = "git@github.com:fuzz-productions/${gitRepo}.git"
			}
			git.clone(gitURL, branch)
			folderName = gitURL.split('/')[1].replace(".git","")
			folderPath = "${folderName}/"

			echo "${currentBuild.fullProjectName}"
			try {
				artifactLocalName = artifactName
				if (env.JOB_NAME.contains("android") && artifactName == "app.zip") {
					artifactLocalName = "app-debug.apk"
				}
				
				copyArtifacts filter: "${artifactLocalName}", flatten: true, projectName: "${currentBuild.fullProjectName}", selector: specific("${env.BUILD_NUMBER}"), target: "${folderName}"
			} catch (Throwable t) {

			}
		}

		if (folderName != '') {
			dir(folderName) {
				body()
			}
		} else {
			body()
		}
		reportExtents(folderPath)
	}
}

/**
 * Extent Reports are these fancy HTML test results, with step-by-step information
 * and pictures and even a pie chart. This method will
 * <ol>
 *     <li>run JUnit</li>
 *     <li>collect all HTML files found in folders called "Extent-Report"</li>
 *     <li>call 'publishHTML' with these files</li>
 *     <li>call {@link slack#uatMessage}</li>
 * </ol>
 *
 * @param folderPath common ancestor of wherever the JUnit testResults are placed
 */
def reportExtents(String folderPath = "") {
	try {
		//Test Reporting
		//${folderPath}**/*.junit, ${folderPath}**/junit-reports/*.xml, ${folderPath}**/junitreports/*.xml, ${folderPath}**/test-results/**/*.xml, ${folderPath}**/*junit.xml,
		junit allowEmptyResults: true, testResults: "${folderPath}**/testng-results.xml, ${folderPath}**/junitreports/TEST*.xml"
	} catch (Exception e) {
		throw e
	}
	if (!slack.hasTest()) {
		currentBuild.result = "UNSTABLE"
	}
	extentReportDir = sh(script: "find . -name Extent-Report -type d", returnStdout: true)
	extentReportDir = extentReportDir.replace("./", "")
	extentReportHtml = sh(script: "ls ${extentReportDir}", returnStdout: true)
	publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "${extentReportDir}", reportFiles: "${extentReportHtml}", reportName: "Extent-Report", reportTitles: ""])
	slack.uatMessage()
}
