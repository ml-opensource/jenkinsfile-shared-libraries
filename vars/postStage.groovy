/**
 * Do nothing if the build failed. Otherwise, execute <code>body()</code>.
 *
 * @param stageName a name to use for this 'post-execution' stage
 * @param body      arbitrary code to run
 * @return nothing
 * @see slack#isProjectSuccessful
 */
def call(String stageName, Closure body = null) {
	stage(stageName) {
		if (currentBuild.result != null && currentBuild.result != "SUCCESS") {
			//DO NOTHING
		} else {
			body()
		}
	}
}