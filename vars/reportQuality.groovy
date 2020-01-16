
/**
 * Report code quality issues within a basic Stage.
 * <p>
 *     This runs a bunch of reporting tools. Configure them with a String
 *     on env called QUALITY_SERVICES. This is intended as a long-term (yet
 *     still unstable) replacement for {@link testStage#call testStage} and
 *     {@link uatStage#call uatStage}.
 * </p>
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @return nothing
 */
def call() {
	stage("Report") {
		final String services = env.QUALITY_SERVICES

		try {
			println "Services: \n- " + services

			List toolset = prebuiltQualityToolset.autodetect(services)


			recordIssues(
					aggregatingResults: true,
					enabledForFailure: true,
					tools: toolset
			)
		} catch (RuntimeException unexpected) {
			println "One problem: ${unexpected.message}"
		}
	}
}

