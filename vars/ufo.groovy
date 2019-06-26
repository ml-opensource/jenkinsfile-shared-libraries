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
	// If the ufo installation should encounter an error, this bit of shell will retry twice more before failing
	bash '''
	for i in 2 1 0
	do
		if go get -u github.com/fuzz-productions/ufo
		then
			exit 0
		fi

		echo "ERROR: problem installing ufo; $i more installation attempt(s) will be made"
		sleep 30
	done
	'''

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
		slack.qbash "ufo deploy --cluster ${cluster} --timeout 15"
	} else {
		slack.qbash "ufo deploy --cluster ${cluster} --config ${config} --timeout 15"
	}
}

def run(String command, String service, String cluster = 'dev', String config = 'default') {
	if (config == 'default') {
		slack.qbash "ufo task run --command '${command}' --cluster ${cluster} --service ${service}"
	} else {
		slack.qbash "ufo task run --command '${command}' --cluster ${cluster} --service ${service} --config ${config}"
	}
}
