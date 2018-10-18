def call(Boolean injectReports = true, Closure body) {
	if (body != null) {
		body()
	}
	gradleClean()
	if (injectReports) {
		androidBuildScriptInject()
	}
	gradleDependencies()
}