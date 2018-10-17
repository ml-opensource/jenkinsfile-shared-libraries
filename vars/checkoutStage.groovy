def call(Closure body) {
	stage("Checkout") {
		checkout scm
		if (body != null) {
			body()
		}
	}
}