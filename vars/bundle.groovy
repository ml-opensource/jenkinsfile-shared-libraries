def install() {
	stage("Install") {
		slack.qbash 'bundle install'
	}
}

def rake(String command = "") {
	slack.qbash "bundle exec rake ${command}"
}