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
	def projects = currentBuild.rawBuild.project
	retString = projects.getName()
	return retString
}
