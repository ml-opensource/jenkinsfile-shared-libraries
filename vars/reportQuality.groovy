import java.util.function.Function

/**
 * This method collates the findings of a set of code quality checks.
 * <p>
 *     If this project has defined <code>env.QUALITY_SERVICES</code>, then
 *     we use the modern quality toolsets.
 * </p>
 * <p>
 *     You can change which of the modern tools are chosen by
 *     <ol>
 *         <li>Passing in a non-null transform function</li>
 *     </ol>
 *     For the exact list of tools please refer to the documentation on {@link reportQuality#basic},
 *     {@link reportQuality#android}, and {@link reportQuality#ios}.
 * </p>
 * BE WARNED: This method suppresses all exceptions.
 *
 * @param translateToToolset something that maps a String into an array of
 * toolset elements; defaults to a method ref to {@link reportQuality#basic}
 * @return nothing
 * @see androidBuildScriptInject#call
 */
def call(Function<String, List> translateToToolset = reportQuality.&basic) {
	try {
		// See if this project opted into a custom set of quality checks
		if (env.QUALITY_SERVICES instanceof String && env.QUALITY_SERVICES.length() > 0) {
			collateIssues(translateToToolset)
		} else {
			legacy()
		}
		sloccountPublish encoding: '', pattern: '**/*cloc.xml'
	} catch (Exception ignored) {
		// Let us silence all errors
	}
}

/**
 * Legacy behavior from {@link reportQuality#call}.
 * <p>
 *      If a caller to that method did not opt into the modern toolset, we simply scan for the following:
 * </p>
 * <ul>
 *     <li>Checkstyle</li>
 *     <li><a href="https://pmd.github.io/">PMD</a></li>
 *     <li>DRY</li>
 *     <li>OpenTasks</li>
 * </ul>
 *
 * @deprecated this is an internal transition method. Please migrate to the modern toolset.
 */
private void legacy() {
	checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*-lint.xml, **/*checkstyle.xml', unHealthy: ''
	pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*pmd.xml', unHealthy: ''
	dry canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*cpd.xml, **/cpdCheck.xml', unHealthy: ''
	openTasks canComputeNew: false, defaultEncoding: '', excludePattern: '**/Libraries/**, **/Pods/**, **/*.framework/**, **/Xcode.app/**, **/build/**', healthy: '', high: 'FIXME,shit,fuck,suck', ignoreCase: true, low: 'deprecated', normal: 'TODO', pattern: '**/*.swift, **/*.java, **/*.kt, **/*.m, **/*.h, **/*.c', unHealthy: ''
}

/**
 * Report code quality issues.
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
 * toolset elements; defaults to a method ref to {@link reportQuality#basic}
 * @return nothing
 * @see reportQuality#android
 */
def collateIssues(Function<String, List> translateToToolset = reportQuality.&basic) {
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


/**
 * This looks for the base set of quality checks.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#collateIssues}.
 * </p>
 *
 * @param services unused
 * @return a list of tools that make sense for this situation
 * @see reportQuality#android
 */
List basic(String services) {
	List toolset = [
			taskScanner(
					excludePattern: '**/build/**, **/node_modules/**, qualityReports/**',
					highTags: 'FIXME,shit,fuck,suck',
					ignoreCase: true,
					includePattern: '**/*.swift, **/*.java, **/*.ts, **/*.kt, **/*.xml, **/*.m, **/*.h, **/*.c, **/*.yml, **/*.gradle',
					lowTags: 'deprecated',
					normalTags: 'TODO'
			),
			// NB: Most linters (including SwiftLint and ESLint) can create CheckStyle-format XML files.
			checkStyle(
					pattern: '**/checkstyle-result.xml, **/checkstyle.xml, **/*-lint.xml',
					reportEncoding: 'UTF-8',
					skipSymbolicLinks: true
			),
			cpd(
					highThreshold: 120,
					pattern: '**/cpd.xml, **/cpdCheck.xml',
					reportEncoding: 'UTF-8',
					skipSymbolicLinks: true
			)
	]
	return toolset
}

/**
 * Choose from a decent set of tools that we typically associate
 * with Android projects.
 * <p>
 *     Strict super-set of {@link reportQuality#basic}.
 * </p>
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#collateIssues}.
 * </p>
 *
 * @param services unused
 * @return a list of tools that make sense for this situation
 */
List android(String services) {
	// By default, we should always look for incomplete tasks (like TODOs).
	List toolset = basic(services)

	[
			javaDoc(),
			androidLintParser(pattern: '**/androidLint.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true),
			pmdParser()
	].forEach {
		toolset.add(it)
	}

	// Minor sanity check in case the plugin API changes significantly
	println "Toolset: " + toolset.getClass()

	return toolset
}

/**
 * Choose from a decent set of tools that we typically associate
 * with iOS projects.
 * <p>
 *     Strict super-set of {@link reportQuality#basic}.
 * </p>
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#collateIssues}.
 * </p>
 *
 * @param services unused
 * @return a list of tools that make sense for this situation
 */
List ios(String services) {
	// By default, we should always look for incomplete tasks (like TODOs).
	List toolset = basic(services)

	[
			clang()
	].forEach {
		toolset.add(it)
	}

	// Minor sanity check in case the plugin API changes significantly
	println "Toolset: " + toolset.getClass()

	return toolset
}

