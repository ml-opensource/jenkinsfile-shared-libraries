def call(Closure body) {
	stage("Echo") {
		slack.echo()
	}
}