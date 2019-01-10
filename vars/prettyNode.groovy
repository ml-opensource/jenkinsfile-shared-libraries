import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;

def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			try {
				println hasPR()
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
	def parent = currentBuild.rawBuild.project.getParent()
	def items = parent.getItems()
	def projectFactory = ((MultiBranchProject) parent).getProjectFactory()
	retString = ""
	for (item in items) {
		if (projectFactory.isProject(item)) {
			def branch = projectFactory.getBranch(item);
			def head = branch.getHead();
			if (head instanceof ChangeRequestSCMHead2) {
				branchName = ((ChangeRequestSCMHead2) head).getOriginName();
				if (branchName.equals(env.BRANCH_NAME)) {
					return true
				}
			}
		}
	}
	return false
}
