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

def run_c(String extras = "", Closure body = null) {
	yarn "run ${extras}" 
}

def test(String extras = "", Closure body = null) {
	yarn "test ${extras}" 
}

def setup(String nodeVersion = 'node', Closure body = null) {
	stage("Configure Environment") {
		bash "nvm install ${nodeVersion}"
		bash 'curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -'
		bash 'echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list'
		yarnInstallWorked = false
		failureCount = 0
		while (!yarnInstallWorked && failureCount<10) {
			try {
				bash 'sudo apt-get update && sudo apt-get install yarn -y'
				yarnInstallWorked = true
			} catch(Throwable e) {
				failureCount++
				sleep 20
			}
		}
		sh 'rm -rf dist || echo "Done"'
		sh 'rm -rf node_modules || echo "Done"'
		if (body != null) {
			body()
		}
	}
}

def installAndSetup() {
	setup()
	install()
}
