/**
 * Send {@link slack#linkMessage Slack} a link to the current build.
 * <p>
 *     We use the current value of <code>env.DEPLOY_URL</code> for that
 *     url.
 * </p>
 *
 * @return nothing
 * @see injectDeploymentVars#call
 */
def call() {
	rtp nullAction: '1', parserName: 'HTML', stableText: "<a href=\"http://${env.DEPLOY_URL}\">http://${env.DEPLOY_URL}</a>"
	deploymentURL = "http://${env.DEPLOY_URL}"
	slack.linkMessage(deploymentURL)
}