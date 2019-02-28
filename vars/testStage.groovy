def call(Boolean isAutomation = false, Closure body) {
	stage("Test") {
		body()
		reportResultsAndCoverage()
		if (isAutomation) {
			slack.uatMessage()
		} else {
			slack.testMessage()
		}
	}
}
