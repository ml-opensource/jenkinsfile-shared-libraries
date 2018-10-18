def call(Closure body) {
	stage("Dependencies") {
		slack.qsh 'fastlane install_dependencies' 
		if (body != null) {
			body()
		}
	}
}