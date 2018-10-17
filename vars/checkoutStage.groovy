def call(Closure body) {
	stage("Checkout") {
		checkout scm
		body()
	}
}