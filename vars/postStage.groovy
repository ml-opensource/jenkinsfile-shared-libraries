def call(String stageName, Closure body = null) {
	stage(stageName) {
		if (currentBuild.result != null && currentBuild.result != "SUCCESS") {
			//DO NOTHING
		} else {
			body()
		}
	}
}