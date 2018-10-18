def call(String inKeys = "", String appName = "", Closure body) {
	fastlaneSetup()
	fastlaneBuild(inKeys, appName)
	fastlaneTest(inKeys)
	fastlaneReport()
}