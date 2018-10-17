def call(Closure body) {
	ansiColor('xterm') {
		withEnv(['LC_ALL=en_US.UTF-8']) {
			body()
		}
	}
}