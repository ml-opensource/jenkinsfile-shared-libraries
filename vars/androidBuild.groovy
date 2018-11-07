def call(Map config, Closure body) {
	prettyNode(config.get('node', 'uber_android')) {
		gradlew.pipeline name: config.get('name', ''), injectReports: config.get('injectReports', true)
	}
}