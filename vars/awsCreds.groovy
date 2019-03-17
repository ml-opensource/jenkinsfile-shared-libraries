/**
 * Execute the code in <code>body</code> under the given AWS profile.
 * <p>
 *     <a href="https://jenkins.io/doc/pipeline/steps/credentials-binding/">
 *         There are more details about all this at the
 *         Credentials Binding Plugin website.
 *     </a>
 * </p>
 * <p>
 *     AWS in this context, of course, refers to the Amazon Web Services
 *     cloud where our Jenkins instance is hosted.
 * </p>
 *
 * @param aws_profile this is used as a <code>credentialsId</code>
 * @param body        a {@link Closure} of code to run
 * @return nothing
 */
def call(String aws_profile, Closure body = null) {
	withCredentials([[
        $class: 'AmazonWebServicesCredentialsBinding',
        accessKeyVariable: 'AWS_ACCESS_KEY_ID',
        credentialsId: "${aws_profile}",
        secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
		body()
	}
}