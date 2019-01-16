def call(Closure body) {
	stage("Checkout") {
		checkout scm
		try {
			sh 'git lfs install'
			sh 'git lfs pull'
		} catch(Throwable t) {
		}
		if (body != null) {
			body()
		}
	}
}
