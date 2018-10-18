def call(String inKeys = "", Closure body) {
	def keys = inKeys.split(",")
	testStage {
		if (body != null) {
			body()
		}
		for(key in keys){
			fastlane "perform_tests key:${key}"
		}
		archiveAppForTesting()
	}
}