
/**
 * Run some code quality checks within a basic Stage.
 * <p>
 *     This runs <code>body()</code> within the Stage, before executing
 *     {@link reportQuality#call}. For similar concepts see
 *     {@link testStage#call testStage} and {@link uatStage#call uatStage}.
 * </p>
 *
 * @param preferredToolset something that returns an array of toolset elements;
 * if null, we will use {@link reportQuality#basic},
 * {@link reportQuality#android}, {@link reportQuality#ios}, and/or
 * {@link reportQuality#web}
 * @param body arbitrary code to run within the stage
 * @return nothing
 */
def call(Closure<List> preferredToolset = null, Closure body) {
	stage("Report") {
		body()
		reportQuality(preferredToolset)
	}
}
