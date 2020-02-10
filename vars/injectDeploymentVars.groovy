/**
 * Define a bunch of environment variables for other scripts to use.
 * <p>
 *     You can retrieve these later with calls to e.g. 'env.BASE_URL'.
 *     Note that this method always defines 'env.IS_WEB' as true - non-web
 *     projects are asked to use alternative build scripts.
 * </p>
 * <p>
 *     Note that future calls to {@link reportQuality#call} may be affected
 *     by the value of `env.IS_WEB`.
 * </p>
 *
 * @param baseURL representation of what, precisely, is being handled by Jenkins
 * here
 * @return nothing
 * @see publishLink#call
 */
def call(String baseURL) {
	namedBranches = ["production", "sandbox", "staging", "dev", "master", "develop", "qa"]
	env.BASE_URL = baseURL
	branchName = env.BRANCH_NAME.replace("-","").replace("_","").replace("/","").toLowerCase()
	env.RAW_ENV = branchName
	endpointPrefix = branchName
	
	env.IS_WEB = "true"

	if (!namedBranches.contains(endpointPrefix)) {
  		target = "dev"
  		if (github.isPR()) {
  			target = env.CHANGE_TARGET.replace("-","_").replace("_","").replace("/","").toLowerCase()
  			if (!namedBranches.contains(target)) {
  				target = "dev"
  			}
  		}
		env.CLEAN_ENV = target
  		env.STANDARD_ENV = false
  		env.ENV_TYPE = "branch"
	} else {
		env.CLEAN_ENV = branchName
		env.STANDARD_ENV = true
		env.ENV_TYPE = endpointPrefix
	}
	if (endpointPrefix == "production" || endpointPrefix == "master") {
		endpointPrefix = ""
		env.IS_PRODUCTION = true
	} else {
		endpointPrefix = endpointPrefix + "."
		env.IS_PRODUCTION = false
	}
	env.DEPLOY_URL="${endpointPrefix}${env.BASE_URL}"
}
