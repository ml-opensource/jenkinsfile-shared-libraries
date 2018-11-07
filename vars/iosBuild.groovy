def call(Map config, Closure body) {
	prettyNode(config.get('node', 'uber_ios')) {
		fastlane.pipeline keys: config.keys, name: config.get('name', '')
	}
}