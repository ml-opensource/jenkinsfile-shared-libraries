def call(String inKeys = "", String appName = "", Closure body) {
	def keys = inKeys.split(",")
	mobileBuildStage(appName) {
		if (body != null) {
			body()
		}
		for(key in keys){
			fastlane "deploy_jenkins key:${key}"
		}
	}
}