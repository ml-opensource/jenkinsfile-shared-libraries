def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	project = currentBuild.rawBuild.project
	sh "echo ${project}"
	node(nodeName) {
		prettyPrintDecorator {
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			body()
		}
	}
}