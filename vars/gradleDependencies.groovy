def call(Closure body) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		gradlew "androidDependencies --refresh-dependencies"
	}
}