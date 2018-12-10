def use(String version = "ruby --latest") {
	stage("Configure Environment") {
		bash "rvm install ${version}"
		bash "rvm use ${version}"
		bash "gem install bundle"
	}
}