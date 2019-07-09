/**
 * Run some code quality checks within a basic Stage.
 * <p>
 *     This runs <code>body()</code> within the Stage, before executing
 *     {@link standardReportArchives}. For similar concepts see
 *     {@link testStage#call testStage} and {@link uatStage#call uatStage}.
 * </p>
 *
 * @param body arbitrary code to run within the stage
 * @return nothing
 */
def call(Closure body) {
	stage("Report") {
		body()
		standardReportArchives()
	}
}
