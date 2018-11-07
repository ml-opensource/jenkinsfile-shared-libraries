def call(String command) {
    slack.qsh "fastlane ${command}"
}

def clean(Closure body = null) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		fastlane 'clean' 
	}
}

def install_certs(Closure body = null) {
	stage("Provision") {
		if (body != null) {
			body()
		}
		fastlane 'install_certs' 
	}
}

def install_dependencies(Closure body = null) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		fastlane 'install_dependencies' 
	}
}

def report(Closure body = null) {
	reportStage {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}

def setup(Closure body = null) {
	if (body != null) {
		body()
	}
	clean()
	install_certs()
	install_dependencies()
}