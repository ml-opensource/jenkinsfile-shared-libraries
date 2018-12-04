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

def install(Closure body = null) {
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
