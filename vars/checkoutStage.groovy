def call(Closure body) {
	stage("Checkout") {
		checkout scm
		sh 'git lfs install'
		sh 'git lfs pull'
		if (body != null) {
			body()
		}
	}
}