def call(Closure body) {
	stage("Clean") {
		slack.qsh 'fastlane clean' 
		if (body != null) {
			body()
		}
	}
}