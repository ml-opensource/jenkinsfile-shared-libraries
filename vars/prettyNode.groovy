import org.jenkinsci.plugins.workflow.job.WorkflowJob

def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			try {
				hasPR()
			} catch(Throwable t) {
				println t
			}
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			body()
		}
	}
}

@NonCPS def hasPR() {
	projects = currentBuild.rawBuild.project.getParent().getItems()
	retString = ""
	for (project in projects) {
		retString = "1"
	}
	return retString
}
