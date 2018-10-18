def call(String appName = "", Closure body) {
	mobileBuildStage(appName) {
		if (body != null) {
			body()
		}
		gradlew "assemble -Pbuild_number=${env.BUILD_NUMBER}"
	}
}