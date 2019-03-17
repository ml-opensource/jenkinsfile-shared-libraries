/**
 * Wrapper around {@link prettyNode#call prettyNode} for Android projects.
 * <p>
 *     By default, they run on the first found node with an 'uber_android' label.
 *     Include a 'name' in the <code>config</code> parameter to override the pipeline
 *     label (see {@link gradlew#pipeline} for details).
 * </p>
 *
 * @param config use this to change what is run and where
 * @param body   this field is ignored
 * @return nothing
 * @see iosBuild
 */
def call(Map config, Closure body) {
	prettyNode(config.get('node', 'uber_android')) {
		gradlew.pipeline name: config.get('name', ''), injectReports: config.get('injectReports', true)
	}
}