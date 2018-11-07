def call(String command) {
    slack.qsh "fastlane ${command}"
}

def clean(Closure body) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		fastlane 'clean' 
	}
}

def install_certs(Closure body) {
	stage("Provision") {
		if (body != null) {
			body()
		}
		fastlane 'install_certs' 
	}
}

def install_dependencies(Closure body) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		fastlane 'install_dependencies' 
	}
}

def report(Closure body) {
	reportStage {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}

def setup(Closure body) {
	if (body != null) {
		body()
	}
	fastlane.clean()
	fastlane.install_certs()
	fastlane.install_dependencies()
}