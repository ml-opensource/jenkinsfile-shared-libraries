def call(Closure body) {
	if (body != null) {
		body()
	}
	fastlaneClean()
	fastlaneProvision()
	fastlaneDependencies()
}