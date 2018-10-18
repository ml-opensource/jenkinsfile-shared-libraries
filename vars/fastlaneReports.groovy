def call(Closure body) {
	reportStage("Clean") {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}