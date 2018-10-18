def call(Closure body) {
	stage("Clean") {
	    gradlew "--stop"
	    if (body != null) {
			body()
		}
	    gradlew "clean"
	}
}