def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			hasPR()
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			body()
		}
	}
}

@NonCPS
def hasPR() {
	projects = currentBuild.rawBuild.project.getParent().getItems()
	for (project in projects) {
		sh "echo ${project.toString()}"
	}
}