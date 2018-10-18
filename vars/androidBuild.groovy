def call(String appName = "", Boolean refresh = false, String nodeName = "uber_android", Closure body) {
	prettyNode(nodeName) {
		gradleBuild(appName, refresh)
	}
}