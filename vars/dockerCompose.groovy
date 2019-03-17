/**
 * Run <a href="https://docs.docker.com/compose/reference/up/">docker-compose up</a> with
 * the given file path.
 * <p>
 *     This starts up the Docker container and all its services. It will send a report
 *     to {@link slack#qbash Slack} if the <code>up</code> command throws an exception.
 * </p>
 *
 * @param file configuration file for the Docker tool
 * @return nothing
 */
def up(String file) {
	stage("Docker Up") {
		// TODO: Why does this not call 'dockerCompose.down()' here?
		bash "docker-compose -f ${file} down"
		bash "docker system prune --all --force"
		bash "docker volume prune --force"
		slack.qbash "docker-compose -f ${file} up -d --build"
	}
}

/**
 * Run <a href="https://docs.docker.com/compose/reference/down/">docker-compose down</a> with
 * the given file path.
 * <p>
 *     This starts up the Docker container and all its services. It will send a report
 *     to {@link slack#qbash Slack} if the <code>up</code> command throws an exception.
 * </p>
 *
 * @param file configuration file for the Docker tool
 * @return nothing
 */
def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
		bash "docker system prune --all --force"
	}
}