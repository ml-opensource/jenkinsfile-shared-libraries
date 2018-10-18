def call(String appName = "", Boolean injectReports = true, String nodeName = "uber_android", Closure body) {
	prettyNode(nodeName) {
		gradleBuild(appName, injectReports)
	}
}