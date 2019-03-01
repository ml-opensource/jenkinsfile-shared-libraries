/**
 * Run a specific command through the local Gradle Wrapper.
 * <p>
 *     Errors are reported to {@link slack#qsh Slack}.
 * </p>
 *
 * @param command usually a Gradle-compatible task, with options and stuff if desired
 * @return nothing
 */
def call(String command) {
    slack.qsh "./gradlew ${command}"
}

/**
 * Assemble all variants of this Gradle project. This can take a while.
 *
 * @param refresh    whether to re-check all the dependencies
 * @param additional extra commands to append to the {@link gradlew#call gradlew} command
 * @param body       arbitrary code to run before gradlew
 * @return nothing
 */
def assemble(Boolean refresh = false, String additional = "", Closure body = null) {
	if (body != null) {
		body()
	}
	if (refresh) {
		gradlew "assemble -Pbuild_number=${env.BUILD_NUMBER} --refresh-dependencies --stacktrace --info ${additional}"
	} else {
		gradlew "assemble -Pbuild_number=${env.BUILD_NUMBER} --stacktrace --info ${additional}"
	}
}

/**
 * Run {@link gradlew#assemble} within a {@link mobileBuildStage}.
 *
 * @param appName    a name for the build stage
 * @param refresh    same as the 'refresh' parameter to {@link gradlew#assemble}
 * @param additional same as the 'additional' parameter to {@link gradlew#assemble}
 * @param body       arbitrary code to run before {@link gradlew#assemble}
 * @return nothing
 */
def assembleStage(String appName = "", Boolean refresh = false, String additional = "", Closure body = null) {
	mobileBuildStage(appName) {
		if (body != null) {
			body()
		}
		assemble(refresh, additional)
	}
}

/**
 * Run <code>body()</code>, then try to stop the Gradle Daemon.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def stop(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "--stop"
}

/**
 * Run <code>body()</code>, then try to clean the Gradle build cache.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def clean(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "clean"
}

/**
 * Run {@link gradlew#stop}, then <code>body()</code>, and then {@link gradlew#clean}, all
 * within a dedicated Stage.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def cleanStage(Closure body = null) {
	stage("Clean") {
	    stop()
	    if (body != null) {
			body()
		}
	    clean()
	}
}

/**
 * Run <code>body()</code>, then try to list out the dependencies of the android Application.
 * <p>
 *     If there is no android application in the present Gradle files, then this will
 *     likely do nothing of use. Make sure to check the settings.gradle if the output
 *     is suspiciously light.
 * </p>
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def dependencies(Closure body = null) {
	if (body != null) {
		body()
	}
	gradlew "androidDependencies --refresh-dependencies"
}

/**
 * Run <code>body()</code>, then {@link gradlew#dependencies}, all within
 * a dedicated build stage.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def dependenciesStage(Closure body = null) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		dependencies()
	}
}

/**
 * Generate a full set of JUnit reports, then run <code>body()</code>.
 *
 * @param body arbitrary code to run after
 * @return nothing
 */
def generateReports(Closure body = null) {
	sh "./gradlew generateReports"
	if (body != null) {
		body()
	}
}

/**
 * Run {@link gradlew#generateReports}, then <code>body()</code>, all within
 * a dedicated report stage.
 *
 * @param body arbitrary code to run after
 * @return nothing
 */
def generateReportsStage(Closure body = null) {
	reportStage {
	    generateReports()
	    if (body != null) {
			body()
		}
	}
}

/**
 * Run <code>body()</code>, then try out the standard test suite(s).
 * <p>
 *     C.f. <a href="https://www.jacoco.org/jacoco/">the JaCoCo web site</a>.
 * </p>
 *
 * @param injectReports true to enable JaCoCo, false to disable it. Defaults to true.
 * @param body          arbitrary code to run first
 * @return nothing
 */
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

/**
 * Run <code>body()</code>, then call {@link gradlew#test}, all within a single
 * test stage.
 *
 * @param injectReports whether to enable JaCoCo
 * @param body          arbitrary code to run first
 * @return nothing
 */
def testStage(Boolean injectReports = true, Closure body = null) {
	testStage {
		if (body != null) {
			body()
		}
		test(injectReports)
	}
}

/**
 * Run, in order:
 * <ol>
 *     <li>body()</li>
 *     <li>{@link gradlew#cleanStage}</li>
 *     <li>{@link androidBuildScriptInject} (but only if injectReports is true)</li>
 *     <li>{@link gradlew#dependenciesStage}</li>
 * </ol>
 *
 * @param injectReports whether to enable JaCoCo
 * @param body          arbitrary code to run first
 * @return nothing
 */
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

/**
 * Run, in order:
 * <ol>
 *     <li>{@link gradlew#setup}</li>
 *     <li>{@link gradlew#assembleStage}</li>
 *     <li>{@link gradlew#testStage}</li>
 *     <li>{@link gradlew#generateReportsStage} (but only if injectReports is true)</li>
 * </ol>
 *
 * @param config use this to indicate whether 'injectReports' should be disabled
 * @param body   this parameter is ignored, and does not run
 * @return nothing
 * @see fastlane#pipeline
 */
def pipeline(Map config, Closure body = null) {
	Boolean injectReports = config.get('injectReports', true)
	setup(injectReports)
	assembleStage(config.get('name', ''))
	testStage(injectReports)
	if (injectReports) {
		generateReportsStage()
	}
}