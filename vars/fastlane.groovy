def call(String command) {
    slack.qsh "fastlane ${command}"
}

def clean(Closure body = null) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		fastlane 'clean reset:true' 
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

def run_reports(Closure body = null) {
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

def perform_tests(String inKeys = "", Closure body = null) {
	def keys = inKeys.split(",")
	if (body != null) {
		body()
	}
	for(key in keys){
		trimmedKey = key.trim()
		fastlane "perform_tests key:${trimmedKey}"
	}
}

def performTestsStage(String inKeys = "", Closure body = null) {
	testStage {
		if (body != null) {
			body()
		}
		perform_tests(inKeys)
		archiveAppForTesting()
	}
}

def build(String inKeys = "", Boolean resign = false, Closure body = null) {
	def keys = inKeys.split(",")
	if (body != null) {
		body()
	}
	for(key in keys){
		trimmedKey = key.trim()
		if (resign) {
			fastlane "build key:${trimmedKey} resign:true"
		} else {
			fastlane "build key:${trimmedKey}"
		}
	}
}

def buildStage(Map config, Closure body = null) {
	mobileBuildStage(config.get('name', '')) {
		if (body != null) {
			body()
		}
		if (!config.release) {
			build(config.keys, config.get('resign', false)) 
		}
	}
}

def pipeline(Map config, Closure body) {
	setup()
	buildStage keys: config.keys, name: config.get('name', ''), resign: config.get('resign', false)
	performTestStage(config.keys)
	report()
}
