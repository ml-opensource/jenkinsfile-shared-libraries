def call(String name = 'uber_ios', Closure body) {
	node(name) {
		ansiColor('xterm') {
			withEnv(['LC_ALL=en_US.UTF-8']) {
				script inScript
			}
		}
	}
}