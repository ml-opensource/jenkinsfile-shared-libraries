def call(String envFile, Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: '.env')]) {
		body()
	}
}