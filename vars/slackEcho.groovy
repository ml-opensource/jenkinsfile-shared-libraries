/**
 * A simple wrapper around {@link slack#echo}, with a dedicated Stage.
 *
 * @param body this parameter is ignored, and does not run
 * @return nothing
 */
def call(Closure body) {
	stage("Echo") {
		slack.echo()
	}
}