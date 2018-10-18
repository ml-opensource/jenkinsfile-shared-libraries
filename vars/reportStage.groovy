def call(Closure body) {
	stage("Report") {
		body()
		standardReportArchives
	}
}