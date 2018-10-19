def call(Closure body) {
	reportStage("Reports") {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}