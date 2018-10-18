def call(Boolean injectReports = true, Closure body) {
	testStage {
		if (body != null) {
			body()
		}
		if (injectReports) {
			gradlew "jacocoDebugTestReport"
		} else {
			gradlew "test"
		}
	}
}