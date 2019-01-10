import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;

def call(String nodeName = "any", Boolean checkoutCode = true, Boolean onlyPR = true, Closure body) {
	isWeb = false
	if (env.IS_WEB == 'true') {
		isWeb = true 
	}
	hasPRJob = hasPR()
	if (!hasPRJob || (hasPRJob && !onlyPR) || isWeb) {
		node(nodeName) {
			prettyPrintDecorator {
				if (checkoutCode) {
					checkoutStage()
					clearChanges()
				}
				body()
			}
		}
	} else {
		println "PR Found not building this branch"
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
