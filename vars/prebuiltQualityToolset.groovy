/**
 * Default behavior: delegate to {@link #basic}.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
 * </p>
 * <p>
 *     This method consumes <code>services</code>.
 * </p>
 *
 * @param services some indication of which tools should be returned
 * @return a list of tools that make sense for this situation
 */
List call(String services) {
	return basic(services)
}

/**
 * This looks for one or so non-committal quality checks.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
 * </p>
 * <p>
 *     This method consumes <code>services</code>.
 * </p>
 *
 * @param services some indication of which tools should be returned
 * @return a list of tools that make sense for this situation
 * @see prebuiltQualityToolset#android
 */
List basic(String services) {
	List toolset = []

	// If this entry is present, scan for TODOs and FIXMEs.
	if (services.remove("taskScanner")) {
		toolset.add(
				taskScanner(excludePattern: '**/build/**, **/node_modules/**, qualityReports/**', highTags: 'FIXME,suck', ignoreCase: true, includePattern: '**/*.swift, **/*.java, **/*.ts, **/*.kt, **/*.xml, **/*.m, **/*.h, **/*.c, **/*.yml, **/*.gradle', lowTags: 'deprecated', normalTags: 'TODO')
		)
	}
	return toolset
}

/**
 * Choose from a decent set of tools that we typically associate
 * with Android projects.
 * <p>
 *     Designed to work well with the Warnings Next Generation
 *     plugin, as executed by e.g. {@link reportQuality#call}.
 * </p>
 * <p>
 *     This method consumes <code>services</code>.
 * </p>
 *
 * @param services some indication of which tools should be returned
 * @return a list of tools that make sense for this situation
 */
List android(String services) {
	// By default, we should always look for JavaDoc errors in the console output.
	List toolset = [
	        javaDoc()
	]
	// Minor sanity check in case the plugin API changes significantly
	println "Toolset: " + toolset.getClass()

	if (services.remove("cpd")) {
		toolset.add(
				cpd(highThreshold: 120, pattern: '**/cpd.xml, **/cpdCheck.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true)
		)
	}
	if (services.remove("checkStyle")) {
		toolset.add(
				checkStyle(pattern: '**/checkstyle-result.xml, **/checkstyle.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true)
		)
	}
	if (services.remove("androidLint")) {
		toolset.add(
				androidLintParser(pattern: '**/androidLint.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true)
		)
	}
	if (services.remove("esLint")) {
		toolset.add(
				esLint(pattern: '**/es-lint.xml', reportEncoding: 'UTF-8', skipSymbolicLinks: true)
		)
	}
	if (services.remove("taskScanner")) {
		toolset.add(
				taskScanner(excludePattern: '**/build/**, **/node_modules/**, qualityReports/**', highTags: 'FIXME,suck', ignoreCase: true, includePattern: '**/*.swift, **/*.java, **/*.ts, **/*.kt, **/*.xml, **/*.m, **/*.h, **/*.c, **/*.yml, **/*.gradle', lowTags: 'deprecated', normalTags: 'TODO')
		)
	}
	if (services.remove("pmd")) {
		toolset.add(
				pmdParser()
		)
	}
	return toolset
}
