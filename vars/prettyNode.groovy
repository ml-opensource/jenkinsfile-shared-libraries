def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	project = currentBuild.rawBuild.project
	node(nodeName) {
		sh "echo ${project.getClass().toString()}"
		prettyPrintDecorator {
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			body()
		}
	}
}