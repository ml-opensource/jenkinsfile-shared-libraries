def call(String command) {
    slack.qbash "yarn ${command}"
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
		bash "nvm install ${nodeVersion}"
		bash 'curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -'
		bash 'echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list'
		bash 'sudo apt-get update && sudo apt-get install yarn -y'
		if (body != null) {
			body()
		}
	}
}
