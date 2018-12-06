def call(String baseURL) {
	env.BASE_URL = baseURL
	env.RAW_ENV = env.BRANCH_NAME
	endpointPrefix = env.BRANCH_NAME
	if (currentEnv != "production" || currentEnv != "staging" || currentEnv != "dev") {
  		env.CLEAN_ENV = "dev"
  		env.STANDARD_ENV = false
	} else {
		env.STANDARD_ENV = true
	}
	if (endpointPrefix == "production") {
		endpointPrefix = ""
		env.IS_PRODUCTION = true
	} else {
		endpointPrefix = endpointPrefix + "."
		env.IS_PRODUCTION = false
	}
	env.DEPLOY_URL="${endpointPrefix}${env.BASE_URL}"
}