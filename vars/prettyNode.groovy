def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			retString = hasPR()
			sh "echo ${retString}"
			body()
		}
	}
}

@NonCPS def hasPR() {
	projects = currentBuild.rawBuild.project.getParent().getItems()
	retString = ""
	for (project in projects) {
		retString = retString + project.toString()
	}
	return retString
}