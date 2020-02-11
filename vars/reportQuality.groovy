
/**
 * This method collates the findings of a set of code quality checks.
 * <p>
 *     To add this to a Pipeline stage, use {@link testStage#call testStage}
 *     or {@link reportStage#call reportStage}.
 * </p>
 * <p>
 *     If this project has defined <code>env.QUALITY_SERVICES</code>, then
 *     we use the modern quality toolsets.
 * </p>
 * <p>
 *     You can change which of the modern tools are chosen by
 *     <ol>
 *         <li>Marking this as ANDROID by {@link gradlew#call running the gradle tool}</li>
 *         <li>Marking this as IOS by {@link fastlane#call running the fastlane tool}</li>
 *         <li>Marking this as WEB by {@link injectDeploymentVars#call aligning to our web toolchain}</li>
 *         or, if you want more control,
 *         <li>Passing in a non-null transform function</li>
 *     </ol>
 *     For the exact list of tools please refer to the documentation on {@link reportQuality#basic},
 *    ~ {@link reportQuality#android}, and {@link reportQuality#ios}.
 * </p>
 * <p>
 *     Regardless of what happens, this method will conclude by collecting any
 *     <a href="https://dwheeler.com/sloccount/">SLOCCount</a>-compatible reports made by
 *     <a href="https://github.com/AlDanial/cloc">cloc</a> that it can find.
 * </p>
 * BE WARNED: This method suppresses all exceptions.
 *
 * @param preferredToolset something that returns an array of toolset elements;
 * if null, we will use {@link reportQuality#basic},
 * {@link reportQuality#android}, {@link reportQuality#ios}, and/or
 * {@link reportQuality#web}
 * @return nothing
 * @see androidBuildScriptInject#call
 */
def call(Closure<List> preferredToolset = null) {
	try {
		// See if this project opted into a custom set of quality checks
		Object check = env.QUALITY_SERVICES
		if (check instanceof String && check.length() > 0) {
			String services = check as String

			println "Using the modern code-path to cater to \'" + services + "\' services."

			Closure<List> translateClosure
			if (preferredToolset == null) {
				// Perform some auto-detection:
				if (env.IS_ANDROID == 'true') {
					translateClosure = { android() }
				} else if (env.IS_IOS == 'true') {
					translateClosure = { ios() }
				} else if (env.IS_WEB == 'true') {
					translateClosure = { web() }
				} else {
					translateClosure = { basic() }
				}
			} else {
				translateClosure = preferredToolset
			}
			collateIssues(translateClosure)
		} else {
			legacy()
		}
		try {
			sloccountPublish encoding: '', pattern: '**/*cloc.xml'
		} catch (Throwable t) {
		}
	} catch (Exception ex) {
		// Let us give voice to all errors
		println "An issue: ${ex.message}"
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
 *     <li>Open Task Scanner</li>
 * </ul>
 *
 * @deprecated this is an internal transition method. Please migrate to the modern toolset.
 */
private void legacy() {
	println "Using the legacy code-path; consider defining a string value for QUALITY_SERVICES in your pipeline's environment."

	checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*-lint.xml, **/*checkstyle.xml', unHealthy: ''
	pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*pmd.xml', unHealthy: ''
	dry canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*cpd.xml, **/cpdCheck.xml', unHealthy: ''
	openTasks canComputeNew: false, defaultEncoding: '', excludePattern: '**/Libraries/**, **/Pods/**, **/*.framework/**, **/Xcode.app/**, **/build/**, **/node_modules/**, qualityReports/**', healthy: '', high: 'FIXME,shit,fuck,suck', ignoreCase: true, low: 'deprecated', normal: 'TODO', pattern: '**/*.swift, **/*.java, **/*.kt, **/*.m, **/*.h, **/*.c', unHealthy: ''
}

/**
 * Report code quality issues.
 * <p>
 *     Implementation detail for {@link reportQuality#call}.
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
 *     The .& syntax in some of the inline code is that of a method reference.
 *     I generally recommend <a href="https://dzone.com/articles/higher-order-functions-groovy-">
 *         Higher-Order Functions with Groovy
 *     </a>
 *     for more information about that.
 * </p>
 *
 * @author Philip Cohn-Cort (Fuzz)
 * @param translateToToolset something that returns an array of
 * toolset elements; defaults to a curried method ref to {@link reportQuality#basic}
 * @return nothing
 * @see reportQuality#android
 */
void collateIssues(Closure<List> translateToToolset) {

	try {

		List toolset = translateToToolset()

		println "Toolset is as follows: ${toolset}"

		recordIssues(
				aggregatingResults: true,
				enabledForFailure: true,
				tools: toolset
		)
	} catch (RuntimeException unexpected) {
		println "One problem: ${unexpected.message}"
		throw unexpected
	}
}


/**
 * This looks for the base set of quality checks.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#collateIssues}.
 * </p>
 * <p>
 *     The returned list will include the following tools:
 *     <ul>
 *         <li>Open Task Scanner</li>
 *         <li>Checkstyle</li>
 *         <li>CPD</li>
 *     </ul>
 * </p>
 *
 * @return a list of tools that make sense for this situation
 * @see reportQuality#android
 * @see reportQuality#ios
 */
@NonCPS
List basic() {
	List toolset = [
			taskScanner(
					excludePattern: '**/Libraries/**, **/Pods/**, **/*.framework/**, **/Xcode.app/**, **/build/**, **/node_modules/**, qualityReports/**',
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

	println "Including the Basic Quality toolset..."

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
 * <p>
 *     The returned list will include the following tools:
 *     <ul>
 *         <li>Everything returned by {@link reportQuality#basic}</li>
 *         <li>JavaDoc Log Scanner</li>
 *         <li>Android Lint</li>
 *         <li><a href="https://pmd.github.io/">PMD</a></li>
 *     </ul>
 * </p>
 *
 * @return a list of tools that make sense for this situation
 */
@NonCPS
List android() {
	// By default, we should always look for incomplete tasks (like TODOs), checkstyle files, and copy-pasted text.
	List toolset = basic()

	[
			javaDoc(),
			androidLintParser(pattern: '**/androidLint.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true),
			pmdParser()
	].forEach {
		toolset.add(it)
	}

	println "Including the default Android Quality toolset..."

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
 * <p>
 *     The returned list will include the following tools:
 *     <ul>
 *         <li>Everything returned by {@link reportQuality#basic}</li>
 *         <li>Clang Log Scanner</li>
 *     </ul>
 * </p>
 *
 * @return a list of tools that make sense for this situation
 */
@NonCPS
List ios() {
	// By default, we should always look for incomplete tasks (like TODOs), checkstyle files, and copy-pasted text.
	List toolset = basic()

	[
			clang()
	].forEach {
		toolset.add(it)
	}

	println "Including the default iOS Quality toolset"

	return toolset
}

/**
 * Choose from a decent set of tools that we typically associate
 * with Web projects.
 * <p>
 *     Strict super-set of {@link reportQuality#basic}.
 * </p>
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#collateIssues}.
 * </p>
 * <p>
 *     The returned list will include the following tools:
 *     <ul>
 *         <li>Everything returned by {@link reportQuality#basic}</li>
 *     </ul>
 * </p>
 *
 * @return a list of tools that make sense for this situation
 */
@NonCPS
List web() {
	// By default, we should always look for incomplete tasks (like TODOs), checkstyle files, and copy-pasted text.
	List toolset = basic()

	println "Including the default Web Quality toolset..."

	return toolset
}

