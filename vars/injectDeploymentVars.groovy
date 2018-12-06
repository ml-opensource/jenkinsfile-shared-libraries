def call(String baseURL) {
	env.BASE_URL = baseURL
	env.RAW_ENV = env.BRANCH_NAME
	if (currentEnv != "production" || currentEnv != "staging" || currentEnv != "dev") {
  		env.CLEAN_ENV = "dev"
	}
	env.DEPLOY_URL="${env.BRANCH_NAME}.${env.BASE_URL}"
}