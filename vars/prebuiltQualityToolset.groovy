/**
 * Default behavior: delegate to {@link #basic}.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
 * </p>
 *
 * @param services unused
 * @return a list of tools that make sense for this situation
 */
List call(String services) {
	return basic(services)
}

/**
 * This looks for the base set of quality checks.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
 * </p>
 *
 * @param services unused
 * @return a list of tools that make sense for this situation
 * @see prebuiltQualityToolset#android
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
 *     Strict super-set of {@link prebuiltQualityToolset#basic}.
 * </p>
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
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
 *     Strict super-set of {@link prebuiltQualityToolset#basic}.
 * </p>
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
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
