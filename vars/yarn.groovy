/**
 * Run a specific command through the local installation of Yarn.
 * <p>
 *     Errors are reported to {@link slack#qbash Slack}.
 * </p>
 *
 * @param command something that yarn may do
 * @return nothing
 */
def call(String command) {
    slack.qbash "yarn ${command}"
}

/**
 * Execute <code>body()</code>, then install any missing dependencies, all
 * within a dedicated Stage.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def install(Closure body = null) {
	stage("Install") {
		if (body != null) {
			body()
		}
		yarn 'install' 
	}
}

/**
 * Execute <code>body()</code>, then build the current project, all
 * within a dedicated Stage.
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def build(Closure body = null) {
	stage("Build") {
		if (body != null) {
			body()
		}
		yarn 'build' 
	}
}

/**
 * In Groovy, 'run' has certain connotations. So this method, which can call
 * <pre>yarn run $extras</pre>,
 * is called run_c instead.
 * <p>
 *     Reference documentation is available
 *     <a href="https://yarnpkg.com/lang/en/docs/cli/run/">here</a>.
 * </p>
 *
 * @param extras this should indicate which scripts to run and with what arguments
 * @param body   this parameter is ignored, and does not run
 * @return nothing
 */
def run_c(String extras = "", Closure body = null) {
	yarn "run ${extras}" 
}

/**
 * Test the current project.
 * <p>
 *     Reference documentation is available
 *     <a href="https://yarnpkg.com/lang/en/docs/cli/test/">here</a>.
 * </p>
 *
 * @param extras extra arguments to the 'test' script (optional)
 * @param body   this parameter is ignored, and does not run
 * @return nothing
 */
def test(String extras = "", Closure body = null) {
	yarn "test ${extras}" 
}

/**
 * Run the standard sequence of commands for recreating a Yarn environment.
 * <p>
 *     This does quite a few things. In brief, it
 *     <ol>
 *         <li>installs Node JS</li>
 *         <li>unconditionally trusts the YarnPkg Public Key</li>
 *         <li>checks for new Debian-compatible Yarn packages</li>
 *         <li>tries up to ten (10) times to install Yarn</li>
 *         <li>deletes both 'dist' and 'node_modules' directories</li>
 *         <li>runs <code>body()</code></li>
 *     </ol>
 *     It does all of this within a single Stage.
 * </p>
 *
 * @param nodeVersion what version of Node JS to enable through NVM
 * @param body        arbitrary code to run at the end
 * @return nothing
 * @see fastlane#setup
 * @see gradlew#setup
 */
def setup(String nodeVersion = 'node', Closure body = null) {
	stage("Configure Environment") {
		bash "nvm install ${nodeVersion}"
		if (nodeVersion != 'node') {
			bash "nvm alias default ${nodeVersion}"
		}
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

/**
 * Simple pipeline simulator: run {@link yarn#setup}, then {@link yarn#install}.
 *
 * @param nodeVersion what version of Node JS should be used by {@link yarn#setup}
 * @return nothing
 */
def installAndSetup(String nodeVersion = 'node') {
	setup(nodeVersion)
	install()
}
