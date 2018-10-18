def call(Closure body) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		slack.qsh 'fastlane install_dependencies' 
	}
}