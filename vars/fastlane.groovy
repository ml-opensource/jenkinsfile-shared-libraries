/**
 * Execute an arbitrary fastlane command.
 * <p>
 *     Fastlane is a tool for building iOS projects. It has
 *     <a href="https://docs.fastlane.tools/">a website with
 *     more information</a> about how to best use it.
 * </p>
 * <p>
 *     Technically this can handle Android projects too, but
 *     we prefer {@link gradlew} for that in practice.
 * </p>
 *
 * @param command something that Fastlane may understand
 * @return nothing
 */
def call(String command) {
    slack.qsh "fastlane ${command}"
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'clean' command.
 * </p>
 * <p>
 *     This runs <code>body()</code> and <code>fastlane clean</code>
 *     within a single Stage.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def clean(Closure body = null) {
	stage("Clean") {
		if (body != null) {
			body()
		}
		fastlane 'clean reset:true' 
	}
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'install_certs' command.
 * </p>
 * <p>
 *     This runs <code>body()</code> and <code>fastlane install_certs</code>
 *     within a single Stage.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def install_certs(Closure body = null) {
	stage("Provision") {
		if (body != null) {
			body()
		}
		fastlane 'install_certs' 
	}
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'pull_secrets' command.
 * </p>
 * <p>
 *     This runs <code>fastlane pull_secrets</code>.
 * </p>
 *
 * @param body this parameter is ignored, and does not run
 * @return nothing
 */
def pull_secrets(Closure body = null) {
	fastlane 'pull_secrets'
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'install_dependencies' command.
 * </p>
 * <p>
 *     This runs <code>body()</code> and <code>fastlane install_dependencies</code>
 *     within a single Stage.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def install_dependencies(Closure body = null) {
	stage("Dependencies") {
		if (body != null) {
			body()
		}
		fastlane 'install_dependencies' 
	}
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'run_reports' command.
 * </p>
 * <p>
 *     This runs <code>fastlane run_reports</code> and <code>body()</code>
 *     within a single {@link reportStage}.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def run_reports(Closure body = null) {
	reportStage {
		sh 'fastlane run_reports'
		if (body != null) {
			body()
		}
	}
}

/**
 * Run the standard sequence of commands for updating a Fastlane environment.
 * <p>
 *     Note that calling this function will first set 'IS_IOS' in the current
 *     execution environment. Future calls to {@link reportQuality#call} may be
 *     affected.
 * </p>
 * <p>
 *     This runs <code>body()</code>, {@link fastlane#clean},
 *     {@link fastlane#install_certs}, and then
 *     {@link fastlane#install_dependencies} within a single Stage.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def setup(Closure body = null) {
	env.IS_IOS = 'true'

	if (body != null) {
		body()
	}
	clean()
	install_certs()
	install_dependencies()
}

/**
 * ?????
 * <p>
 *     The fastlane site has no documentation for the 'perform_tests' command.
 * </p>
 * <p>
 *     This runs <code>body()</code> and multiple instances of
 *     <code>fastlane perform_tests</code>.
 * </p>
 *
 * @param inKeys comma-separated string of keys, each one used for a single
 * invocation of 'perform_tests'
 * @param body   arbitrary code to run
 * @return nothing
 */
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

/**
 * Execute {@link #perform_tests} within a {@link testStage}.
 *
 * @param inKeys comma-separated string of keys, each one used for a single
 * invocation of 'perform_tests'
 * @param body   arbitrary code to run before the tests
 * @return nothing
 */
def performTestsStage(String inKeys = "", Closure body = null) {
	testStage {
		if (body != null) {
			body()
		}
		perform_tests(inKeys)
		archiveAppForTesting()
	}
}

/**
 * Build the 'build' lane of the current Fastlane project.
 * <p>
 *     If the <code>resign</code> parameter is true, this enables
 *     the 'resign' flag. No idea what that's supposed to do, as useful
 *     documentation is antithetical to the Fastlane ethos.
 * </p>
 * <p>
 *     This runs <code>body()</code> and then one execution of
 *     <code>fastlane build</code> for each key in <code>inKeys</code>.
 * </p>
 *
 * @param inKeys a comma-separated list of keys, each used for one call
 * to <code>fastlane build</code>
 * @param resign whether to include a <code>resign:true</code> parameter in
 * the fastlane call
 * @param body arbitrary code to run
 * @return nothing
 */
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

/**
 * {@link fastlane#build Build} the current fastlane config within a dedicated
 * {@link mobileBuildStage}.
 * <p>
 *     This runs <code>body()</code> within the Stage, before executing
 *     the build. If <code>config.release</code> returns true, the actual
 *     build step will be skipped.
 * </p>
 *
 * @param config key-value pairs for defining the build stage and whether
 * 're-signing' should be enabled
 * @param body   arbitrary code to run
 * @return nothing
 */
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

/**
 * Execute a standard Fastlane sequence of events.
 * <p>
 *     This runs {@link fastlane#setup setup()},
 *     {@link fastlane#buildStage buildStage()},
 *     <code>performTestStage()</code>,
 *     then <code>report()</code>.
 * </p>
 *
 * @param config use this to provide the keys and build name used by
 * {@link fastlane#buildStage}
 * @param body   this parameter is ignored, and does not run
 * @return nothing
 * @see gradlew#pipeline
 */
def pipeline(Map config, Closure body) {
	setup()
	buildStage keys: config.keys, name: config.get('name', ''), resign: config.get('resign', false)
	performTestStage(config.keys)
	report()
}
