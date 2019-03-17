/**
 * Execute privileged code.
 * <ol>
 *     <li>Rename an env file into the current directory, with name '.env'</li>
 *     <li>Run <code>body()</code></li>
 *     <li>Delete the env file</li>
 * </ol>
 *
 * @param envFile the name of the type of environment in use
 * @param body    code to run in that context
 * @return nothing
 */
def call(String envFile, Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env"
		sh "chmod 600 .env"
		body()
		sh "rm .env"
	}
}

/**
 * Execute privileged code.
 * <ol>
 *     <li>Rename an env file into the current directory, with name '.env${ending}'</li>
 *     <li>Run <code>body()</code></li>
 *     <li>Delete the env file</li>
 * </ol>
 *
 * @param envFile the name of the type of environment in use
 * @param ending  a suffix for the local copy of the env file
 * @param body    code to run in that context
 * @return nothing
 */
def withSuffix(String envFile, String ending = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE .env${ending}"
		sh "chmod 600 .env${ending}"
		body()
		sh "rm .env${ending}"
	}
}

/**
 * Execute privileged code.
 * <ol>
 *     <li>Rename an env file into the current directory, with name '${starting}.env'</li>
 *     <li>Run <code>body()</code></li>
 *     <li>Delete the env file</li>
 * </ol>
 *
 * @param envFile  the name of the type of environment in use
 * @param starting a prefix for the local copy of the env file
 * @param body     code to run in that context
 * @return nothing
 */
def withPrefix(String envFile, String starting = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE ${starting}.env"
		sh "chmod 600 ${starting}.env"
		body()
		sh "rm ${starting}.env"
	}
}

/**
 * Execute privileged code.
 * <ol>
 *     <li>Rename an env file into the current directory, with name '${filePath}.env'</li>
 *     <li>Run <code>body()</code></li>
 *     <li>Delete the env file</li>
 * </ol>
 *
 * @param envFile  the name of the type of environment in use
 * @param filePath a directory for the local copy of the env file
 * @param body     code to run in that context
 * @return nothing
 */
def atPath(String envFile, String filePath = '', Closure body = null) {
	withCredentials([file(credentialsId: "${envFile}", variable: 'ENVFILE')]) {
		sh "mv $ENVFILE ${filePath}/.env"
		sh "chmod 600 ${filePath}/.env"
		body()
		sh "rm ${filePath}/.env"
	}
}
