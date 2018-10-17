def call(Closure body) {
	stage("Build") {
		body()
		storeFuzzArtifacts()
		slack.buildMessage()
	}
}