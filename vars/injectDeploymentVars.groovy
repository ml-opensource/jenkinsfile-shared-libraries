def call(String baseURL) {
	env.BASE_URL = baseURL
	env.RAW_ENV = env.BRANCH_NAME
	endpointPrefix = env.BRANCH_NAME
	if (currentEnv != "production" || currentEnv != "staging" || currentEnv != "dev") {
  		env.CLEAN_ENV = "dev"
	}
	if (endpointPrefix == "production") {
		endpointPrefix = ""
	} else {
		endpointPrefix = endpointPrefix + "."
	}
	env.DEPLOY_URL="${endpointPrefix}${env.BASE_URL}"
}