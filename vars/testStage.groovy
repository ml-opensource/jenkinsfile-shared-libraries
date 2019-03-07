/**
 * Run local tests over the current build.
 * <p>
 *     This executes <code>body()</code>, then {@link reportResultsAndCoverage#call},
 *     and then finally {@link slack#testMessage}, all within a single dedicated
 *     Stage. For most projects, that's enough to execute a full test suite and
 *     see the results in slack.
 * </p>
 * <p>
 *     To run tests which live in an entirely separate git repository (i.e. most
 *     of our Automation suites), use {@link uatStage#call} instead.
 * </p>
 * <p>
 *     Android projects will invoke this during execution of {@link androidBuild#call}
 *     and iOS projects will invoke this during execution of {@link iosBuild#call}.
 * </p>
 *
 * @param body arbitrary code to run first
 * @return nothing
 */
def call(Closure body) {
	stage("Test") {
		body()
		reportResultsAndCoverage()
		slack.testMessage()
	}
}
