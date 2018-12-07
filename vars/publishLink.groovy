def call() {
	rtp nullAction: '1', parserName: 'HTML', stableText: "<a href=\"http://${env.DEPLOY_URL}\">http://${env.DEPLOY_URL}</a>"
	deploymentURL = "http://${env.DEPLOY_URL}"
	slack.linkMessage(deploymentURL)
}