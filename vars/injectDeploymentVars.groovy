def call(String baseURL) {
	env.BASE_URL = baseURL
	env.RAW_ENV = env.BRANCH_NAME
	endpointPrefix = env.BRANCH_NAME

	if (github.isPR()) {
		end.RAW_ENV = env.CHANGE_BRANCH
		endpointPrefix = env.CHANGE_BRANCH
	}

	if (endpointPrefix != "production" || endpointPrefix != "staging" || endpointPrefix != "dev") {
  		env.CLEAN_ENV = "dev"
  		env.STANDARD_ENV = false
  		env.ENV_TYPE = "branch"
	} else {
		env.STANDARD_ENV = true
		env.ENV_TYPE = endpointPrefix
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