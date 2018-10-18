def call(Closure body) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		slack.qsh 'fastlane clean' 
	}
}