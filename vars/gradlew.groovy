def call(String command) {
    slack.qsh "./gradlew ${command}"
}

def assemble(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "assemble -Pbuild_number=${env.BUILD_NUMBER}"
}

def assembleStage(String appName = "", Closure body = null) {
	mobileBuildStage(appName) {
		if (body != null) {
			body()
		}
		assemble()
	}
}

def clean(Closure body = null) {
	stage("Clean") {
	    gradlew "--stop"
	    if (body != null) {
			body()
		}
	    gradlew "clean"
	}
}

def dependencies(Closure body) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		gradlew "androidDependencies --refresh-dependencies"
	}
}

def generateReports(Closure body) {
	reportStage {
	    sh "./gradlew generateReports"
	    if (body != null) {
			body()
		}
	}
}

def test(Boolean injectReports = true, Closure body) {
	testStage {
		if (body != null) {
			body()
		}
		if (injectReports) {
			gradlew "jacocoDebugTestReport"
		} else {
			gradlew "test"
		}
	}
}

def setup(Boolean injectReports = true, Closure body) {
	if (body != null) {
		body()
	}
	clean()
	if (injectReports) {
		androidBuildScriptInject()
	}
	dependencies()
}

def pipeline(Map config, Closure body) {
	Boolean injectReports = config.get('injectReports', true)
	setup(injectReports)
	assembleStage(config.get('name', ''))
	test(injectReports)
	if (injectReports) {
		generateReports()
	}
}