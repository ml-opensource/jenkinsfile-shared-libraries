def call(String envFile, String ending = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env${ending}"
		body()
		sh "rm .env${ending}"
	}
}