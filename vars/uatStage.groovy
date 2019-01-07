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
			folderName = gitUrl.split('/')[1].replace(".git","")
			folderPath = "${folderName}/"

			copyArtifacts filter: 'app.zip', projectName: "${env.fullProjectName}", selector: specific("${env.BUILD_NUMBER}"), target: "${folderName}"
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
		if (slack.getTestSummary() == "No tests found") {
			currentBuild.result = "UNSTABLE"
		}
		publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "${folderPath}Extent-Report", reportFiles: ‘index.html’, reportName: ‘Extent-Report’, reportTitles: ‘’])
		slack.uatMessage()
	}
}