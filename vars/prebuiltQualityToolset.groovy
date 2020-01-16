/**
 * Default behavior: delegate to {@link #autodetect}.
 * <p>
 *     This method consumes <code>services</code>.
 * </p>
 *
 * @param services some indication of which tools should be returned
 * @return a list of tools that make sense for this situation
 */
List call(String services) {
	return autodetect(services)
}

/**
 * TODO: Autodetect the best toolset.
 * <p>
 *     This method consumes <code>services</code>.
 * </p>
 *
 * @param services some indication of which tools should be returned
 * @return a list of tools that make sense for this situation
 */
List autodetect(String services) {
	if (env.PLATFORM == 'Android') {
		return android(services)
	} else {
		// No tools to discuss...return a default set
		return []
	}
}

/**
 * Choose from a decent set of tools that we typically associate
 * with Android projects.
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

	// TODO: Add some tools!

	return toolset
}
