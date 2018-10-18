def call(Closure body) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		fastlane 'clean' 
	}
}