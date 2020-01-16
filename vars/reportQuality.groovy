
/**
 * Report code quality issues.
 * <p>
 *     This runs a bunch of reporting tools. Configure them with a String
 *     on env called QUALITY_SERVICES. This is intended as a long-term (yet
 *     still unstable) replacement for {@link testStage#call testStage} and
 *     {@link uatStage#call uatStage}. To add it to a Pipeline stage, use
 *     {@link reportStage#call reportStage}.
 * </p>
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @return nothing
 */
def call() {
	final String services = env.QUALITY_SERVICES

	try {
		println "Services: \n- " + services

		List toolset = prebuiltQualityToolset.basic(services)


		recordIssues(
				aggregatingResults: true,
				enabledForFailure: true,
				tools: toolset
		)
	} catch (RuntimeException unexpected) {
		println "One problem: ${unexpected.message}"
	}
}

