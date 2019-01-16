def call(String gitRepo = "", String branch = "", Closure body) {
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
				copyArtifacts filter: 'app.zip', projectName: "${currentBuild.fullProjectName}", selector: specific("${env.BUILD_NUMBER}"), target: "${folderName}"
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
		try {
			//Test Reporting
			junit allowEmptyResults: true, testResults: "${folderPath}**/*.junit, ${folderPath}**/junit-reports/*.xml, ${folderPath}**/test-results/**/*.xml, ${folderPath}**/TEST*.xml, ${folderPath}**/*junit.xml, ${folderPath}**/testng-results.xml"
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
}
