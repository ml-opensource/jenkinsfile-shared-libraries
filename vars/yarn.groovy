def call(String command) {
    slack.qsh "yarn ${command}"
}

def install(Closure body = null) {
	stage("Install") {
		if (body != null) {
			body()
		}
		yarn 'install' 
	}
}

def build(Closure body = null) {
	stage("Build") {
		if (body != null) {
			body()
		}
		yarn 'build' 
	}
}

def test(Closure body = null) {
	stage("Test") {
		if (body != null) {
			body()
		}
		yarn 'test' 
	}
}

def setup(String nodeVersion = 'node', Closure body = null) {
	stage("Configure Environment") {
		sh "nvm install ${nodeVersion}"
		sh 'curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -'
		sh 'echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list'
		sh 'sudo apt-get update && sudo apt-get install yarn -y'
		if (body != null) {
			body()
		}
	}
}
