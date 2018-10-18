def call(String keys = "", String appName = "", String nodeName = "uber_ios", Closure body) {
	prettyNode(nodeName) {
		fastlaneBuild(keys, appName)
	}
}