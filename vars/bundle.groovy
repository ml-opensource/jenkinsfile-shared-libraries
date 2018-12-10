def install() {
	stage("Install") {
		bash 'bundle install'
	}
}

def rake(String command = "") {
	bash "bundle exec rake ${command}"
}