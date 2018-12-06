def call(String aws_profile, Closure body = null) {
	withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        credentialsId: "${aws_profile}",
        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
		body()
	}
}