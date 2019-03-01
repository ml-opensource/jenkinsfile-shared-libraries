/**
 * The standard 'build' stage for mobile apps. Not recommended for website-styled projects.
 * <p>
 *     The name to use is determined before this stage runs. Inside the stage, we execute
 *     <code>body()</code>, then archive artifacts created during that call, then send out
 *     a {@link slack#buildMessage 'build' message through slack}. Lastly, the downstream
 *     project 'Fuzzwares Push Notifications' is asked to run, and it is this that signals
 *     the end of the build.
 * </p>
 *
 * @param prettyName an override for the value of {@link slack#jobName}, used for Fuzzwares
 * @param body       arbitrary code to run within
 * @return nothing
 */
def call(String prettyName = "", Closure body) {
	def jobName = slack.jobName()
	def prettyJobName = prettyName
	if (prettyJobName.trim().equals("")) {
		prettyJobName = jobName
	}
	stage("Build") {
		body()
		archiveArtifacts '**/archive/*.ipa, **/outputs/apk/**/*.apk'
		storeFuzzArtifacts()
		slack.buildMessage()
		build job: 'Fuzzwares Push Notifications', parameters: [string(name: 'JOB', value: jobName), string(name: 'NAME', value: prettyJobName), string(name: 'BUILD', value: "${env.BUILD_NUMBER}")], wait: false
	}
}