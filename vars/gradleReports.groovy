def call(Closure body) {
	reportStage {
	    sh "./gradlew generateReports"
	    if (body != null) {
			body()
		}
	}
}