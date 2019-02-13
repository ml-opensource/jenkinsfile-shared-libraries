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
		try {
			//Test Reporting
			//${folderPath}**/*.junit, ${folderPath}**/junit-reports/*.xml, ${folderPath}**/junitreports/*.xml, ${folderPath}**/test-results/**/*.xml, ${folderPath}**/*junit.xml, 
			junit allowEmptyResults: true, testResults: "${folderPath}**/testng-results.xml, ${folderPath}**/junitreports/*.xml, ${folderPath}**/TEST*.xml"
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
