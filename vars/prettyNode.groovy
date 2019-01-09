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
	project = currentBuild.rawBuild.project.getParent()
	sh "echo ${project}"
}