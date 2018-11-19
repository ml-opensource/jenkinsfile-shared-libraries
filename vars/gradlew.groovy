def call(String command) {
    slack.qsh "./gradlew ${command}"
}

def assemble(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "assemble -Pbuild_number=${env.BUILD_NUMBER} --stacktrace --info"
}

def assembleStage(String appName = "", Closure body = null) {
	mobileBuildStage(appName) {
		if (body != null) {
			body()
		}
		assemble()
	}
}

def stop(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "--stop"
}

def clean(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "clean"
}

def cleanStage(Closure body = null) {
	stage("Clean") {
	    stop()
	    if (body != null) {
			body()
		}
	    clean()
	}
}

def dependencies(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "androidDependencies --refresh-dependencies"
}

def dependenciesStage(Closure body = null) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		dependencies()
	}
}

def generateReports(Closure body = null) {
	sh "./gradlew generateReports"
	if (body != null) {
		body()
	}
}

def generateReportsStage(Closure body = null) {
	reportStage {
	    generateReports()
	    if (body != null) {
			body()
		}
	}
}

def test(Boolean injectReports = true, Closure body = null) {
	if (body != null) {
		body()
	}
	if (injectReports) {
		gradlew "jacocoDebugTestReport"
	} else {
		gradlew "test"
	}
}

def testStage(Boolean injectReports = true, Closure body = null) {
	testStage {
		if (body != null) {
			body()
		}
		test(injectReports)
	}
}

def setup(Boolean injectReports = true, Closure body = null) {
	if (body != null) {
		body()
	}
	cleanStage()
	if (injectReports) {
		androidBuildScriptInject()
	}
	dependenciesStage()
}

def pipeline(Map config, Closure body = null) {
	Boolean injectReports = config.get('injectReports', true)
	setup(injectReports)
	assembleStage(config.get('name', ''))
	testStage(injectReports)
	if (injectReports) {
		generateReportsStage()
	}
}