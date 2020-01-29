import java.util.function.Function

/**
 * Report code quality issues.
 * <p>
 *     This runs a bunch of reporting tools. Configure them with a String
 *     on env called QUALITY_SERVICES. This is intended as a long-term (yet
 *     still unstable) replacement for
 *     {@link standardReportArchives#call standardReportArchives}
 * </p>
 * <p>
 *     To add it to a Pipeline stage, use {@link testStage#call testStage}
 *     or {@link reportStage#call reportStage}.
 * </p>
 * <p>
 *     The 'toolset' returned by the <code>translateToToolset</code> param
 *     must conform to the 'tools' property of the 'recordIssues' command,
 *     which this function calls. See
 *     <a href="https://github.com/jenkinsci/warnings-ng-plugin/blob/master/doc/Documentation.md">
 *         the official 'Warnings Next Generation' docs
 *     </a> for a detailed introduction to the syntax.
 * </p>
 * <p>
 *     The .& syntax in our default parameter is that of a method reference.
 *     I generally recommend <a href="https://dzone.com/articles/higher-order-functions-groovy-">
 *         Higher-Order Functions with Groovy
 *     </a>
 *     for more information about that.
 * </p>
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @param translateToToolset something that maps a String into an array of
 * toolset elements; defaults to a method ref to {@link prebuiltQualityToolset#basic}
 * @return nothing
 * @see prebuiltQualityToolset#android
 */
def call(Function<String, List> translateToToolset = prebuiltQualityToolset.&basic) {
	final String services = env.QUALITY_SERVICES

	try {
		println "Services: \n- " + services

		List toolset = translateToToolset(services)

		println "Toolset is as follows: ${toolset}"

		recordIssues(
				aggregatingResults: true,
				enabledForFailure: true,
				tools: toolset
		)
	} catch (RuntimeException unexpected) {
		println "One problem: ${unexpected.message}"
	}
}

