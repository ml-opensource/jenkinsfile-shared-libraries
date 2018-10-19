def call(Closure body) {
	reportStage {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}