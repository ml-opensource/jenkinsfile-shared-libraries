def call(Closure body) {
	if (body != null) {
		body()
	}
	fastlane.clean()
	fastlane.install_certs()
	fastlane.install_dependencies()
}