/**
 * Send both {@link slack#linkMessage Slack} and Jenkins a link to the current build.
 * <p>
 *     We use the current value of <code>env.DEPLOY_URL</code> for that
 *     url.
 * </p>
 *
 * @return nothing
 * @see injectDeploymentVars#call
 */
def call() {
    // Publish a Rich Text Publishing message - this will appear on the Build Status page.
	// See https://jenkins.io/doc/pipeline/steps/rich-text-publisher-plugin/ for syntax
	rtp(
			nullAction: '1',
			parserName: 'HTML',
			stableText: "<a href=\"http://${env.DEPLOY_URL}\">http://${env.DEPLOY_URL}</a>"
	)

    // Publish a plain URL to our Slack integration. That'll handle formatting for us.
	deploymentURL = "http://${env.DEPLOY_URL}"
	slack.linkMessage(deploymentURL)
}