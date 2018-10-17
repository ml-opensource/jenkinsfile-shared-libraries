def call(Closure body) {
	stage("Test") {
		body()
		slack.testMessage()
	}
}