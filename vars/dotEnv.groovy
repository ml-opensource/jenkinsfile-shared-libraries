def call(String envFile, Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env"
		body()
		sh "rm .env"
	}
}