def call(String prettyName = "", Closure body) {
	def jobName = slack.jobName()
	def prettyJobName = prettyName
	if (prettyJobName.trim().equals("")) {
		prettyJobName = jobName
	}
	stage("Build") {
		body()
		archiveArtifacts '**/archive/*.ipa, **/output/**/*.apk'
		storeFuzzArtifacts()
		slack.buildMessage()
		build job: 'Fuzzwares Push Notifications', parameters: [string(name: 'JOB', value: jobName), string(name: 'NAME', value: prettyJobName), string(name: 'BUILD', value: "${env.BUILD_NUMBER}")], wait: false
	}
}