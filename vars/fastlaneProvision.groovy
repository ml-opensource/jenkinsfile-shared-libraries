def call(Closure body) {
	stage("Provision") {
		slack.qsh 'fastlane install_certs' 
		if (body != null) {
			body()
		}
	}
}