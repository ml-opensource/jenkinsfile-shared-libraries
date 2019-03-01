/**
 * Wrapper around {@link prettyNode#call prettyNode} for iOS projects.
 * <p>
 *     By default, they run on the first found node with an 'uber_ios' label.
 *     Include a 'name' in the <code>config</code> parameter to override the pipeline
 *     label (see {@link fastlane#pipeline} for details).
 * </p>
 *
 * @param config use this to change what is run and where
 * @param body   this field is ignored
 * @return nothing
 * @see androidBuild
 */
def call(Map config, Closure body) {
	prettyNode(config.get('node', 'uber_ios')) {
		fastlane.pipeline keys: config.keys, name: config.get('name', '')
	}
}