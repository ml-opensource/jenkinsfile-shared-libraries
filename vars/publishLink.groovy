/**
 * Send both {@link slack#linkMessage Slack} and Jenkins a link to the current build.
 * <p>
 *     We use the current value of <code>env.DEPLOY_URL</code> for that
 *     url.
 * </p>
 *
 * @param urlScheme what scheme the url should use. Defaults to https
 * @return nothing
 * @see injectDeploymentVars#call
 */
def call(String urlScheme = "https", String url = '') {

	// First, define a common 'deployment url' where the application can be found.
	String deploymentURL = "${urlScheme}://"
	if ( url != '' ) {
		deploymentURL = deploymentURL + url
	}
	else {
		deploymentURL = deploymentURL + env.DEPLOY_URL
	}


    // Publish a Rich Text Publishing message - this will appear on the Build Status page.
	// See https://jenkins.io/doc/pipeline/steps/rich-text-publisher-plugin/ for syntax
	rtp(
		nullAction: '1',
		parserName: 'HTML',
		stableText: "<a href=\"${deploymentURL}\">${deploymentURL}</a>"
	)

    // Publish a plain URL to our Slack integration. That'll handle formatting for us.
	slack.linkMessage(deploymentURL)
}