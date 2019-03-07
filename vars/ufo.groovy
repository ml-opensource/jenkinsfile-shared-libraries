/**
 * Install UFO locally to this machine.
 * <p>
 *     Check out the <a href="https://github.com/fuzz-productions/ufo/blob/master/README.md">
 *         UFO readme</a>
 *     for more details.
 * </p>
 *
 * @return nothing
 */
def install() {
	bash "go get -u github.com/fuzz-productions/ufo"
}

/**
 * Deploy the current build to Amazon's ECS (Elastic Cloud Storage) service.
 *
 * @param cluster an ECS cluster. Should match one defined in the config file
 * @param config  preferred configuration file (by default, UFO will look for config.json)
 * @return nothing
 */
def deploy(String cluster = 'dev', String config = 'default') {
	if (config == 'default') {
		slack.qbash "ufo deploy --cluster ${cluster} --timeout 10"
	} else {
		slack.qbash "ufo deploy --cluster ${cluster} --config ${config} --timeout 10"
	}
}