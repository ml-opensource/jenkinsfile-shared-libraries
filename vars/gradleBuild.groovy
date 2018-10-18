def call(String appName = "", Boolean injectReports = true, Closure body) {
	gradleSetup(injectReports)
	gradleAssemble(appName)
	gradleTest(injectReports)
	if (injectReports) {
		gradleReports()
	}
}