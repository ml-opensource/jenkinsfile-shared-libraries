def call(Closure body) {
	stage("Test") {
		sh 'rm -rf reports || echo "Done"'
		sh 'mkdir reports'
		body()
		reportResultsAndCoverage()
		slack.testMessage()
	}
}