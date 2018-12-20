def call(String envFile, Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env${ending}"
		sh "chmod 600 .env${ending}"
		body()
		sh "rm .env${ending}"
	}
}

def withSuffix(String envFile, String ending = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env${ending}"
		sh "chmod 600 .env${ending}"
		body()
		sh "rm .env${ending}"
	}
}

def withPrefix(String envFile, String starting = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE ${starting}.env"
		sh "chmod 600 ${starting}.env"
		body()
		sh "rm ${starting}.env"
	}
}