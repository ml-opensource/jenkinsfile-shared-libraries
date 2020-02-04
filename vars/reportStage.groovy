import java.util.function.Function

/**
 * Run some code quality checks within a basic Stage.
 * <p>
 *     This runs <code>body()</code> within the Stage, before executing
 *     {@link reportQuality#call}. For similar concepts see
 *     {@link testStage#call testStage} and {@link uatStage#call uatStage}.
 * </p>
 *
 * @param translateToToolset something that maps a String into an array of
 * toolset elements; if null, we will use {@link reportQuality#basic},
 * {@link reportQuality#android}, and/or {@link reportQuality#ios}
 * @param body arbitrary code to run within the stage
 * @return nothing
 */
def call(Function<String, List> translateToToolset = null, Closure body) {
	stage("Report") {
		body()
		reportQuality(translateToToolset)
	}
}
