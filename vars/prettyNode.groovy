import org.jenkinsci.plugins.workflow.job.WorkflowJob
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;

/**
 * Wrapper around the Jenkins built-in 'node()' function. Next to
 * zero relation to the widely-known 'Node JS' programming library.
 * <p>
 *     The variant of the 'node()' function called here requests
 *     {@link prettyPrintDecorator#call auto-colored output} before
 *     executing <code>body()</code>.
 * </p>
 * <p>
 *     Essential to this class is the concept of the 'protected'
 *     branch. This term refers to a VCS branch that undergoes
 *     additional scrutiny, like 'dev', 'master', or 'production'.
 *     We work from an assumption that if a project makes use of
 *     Pull Requests (PRs), then code may only be added to a
 *     protected branch by merging a PR into said branch.
 * </p>
 * <p>
 *     This method will always run 'node()' if the current branch is
 *     protected. If the current branch is <em>NOT</em> protected,
 *     then we will try to ensure that exactly one build of the code
 *     contained in that branch is built. When two requested builds
 *     cover equivalent code, this method tries to prioritize
 *     whichever is a PR (you can control this behavior with
 *     <code>onlyPr</code>).
 * </p>
 *
 * @param nodeName     what machine to run on. Can be a node label
 * @param checkoutCode whether to execute {@link checkoutStage#call}
 * and {@link clearChanges#call} before <code>body()</body>
 * @param onlyPR       whether to avoid duplicate builds
 * @param body         code to run within the node, after the checkout
 * @return nothing
 */
def call(String nodeName = "", Boolean checkoutCode = true, Boolean onlyPR = true, Closure body) {
	isWeb = false
	if (env.IS_WEB == 'true') {
		isWeb = true 
	}
	hasPRJob = hasPR()
	protectedBranches = ["dev","master","production","staging","sandbox"]
	isProtectedBranch = protectedBranches.contains(env.BRANCH_NAME) || registerBranches.contains(env.BRANCH_NAME);
	if (!hasPRJob || (hasPRJob && !onlyPR) || isWeb || isProtectedBranch) {
		try {
			node(nodeName) {
				prettyPrintDecorator {
					if (checkoutCode) {
						checkoutStage()
						clearChanges()
					}
					if (nodeName == 'jenkins-ecs'){
						bash "sudo setfacl -m user:jenkins:rw /var/run/docker.sock"
					}
					body()
				}
			}
		} catch(Throwable e) {
			if (isMultibranch()) {
				slack.sendSlackError(e, "Unknown failure detected during _*Stage ${env.STAGE_NAME}*_")
			}
        	throw e
		}
	} else {
		println "PR Found not building this branch"
	}
}

/**
 * Internal function for {@link prettyNode#call}.
 * <p>
 *     This method checks for Parent projects within the build
 *     environment. If one is found of type MultiBranchProject,
 *     then this method returns true. If not, this returns false.
 * </p>
 * <p>
 *     I'm afraid I don't really understand why this matters, but
 *     hopefully
 *     <a href="https://wiki.jenkins.io/display/JENKINS/Multi-Branch+Project+Plugin">
 *         this deprecated plugin</a>
 *     and <a href="https://plugins.jenkins.io/workflow-multibranch">
 *         this web page</a>
 *     can shed some light.
 * </p>
 *
 * @return a boolean reflecting this project, as described above
 */
@NonCPS def isMultibranch() {
	try {
		def parent = currentBuild.rawBuild.project.getParent()
		def items = parent.getItems()
		def projectFactory = ((MultiBranchProject) parent).getProjectFactory()
		return true
	} catch (Exception e) {
		return false
	}
}

/**
 * Internal function for {@link prettyNode#call}.
 * <p>
 *     This method iterates over other builds in the greater project
 *     environment. If any of those are of type ChangeRequestSCMHead2,
 *     then we know that particular build represents a Pull Request.
 *     Ideally, we will find a Pull Request whose 'origin' branch is
 *     equivalent to <code>env.BRANCH_NAME</code>.
 * </p>
 * <p>
 *     This method returns one of two values: true if such a Pull
 *     Request was found, false in all other cases.
 * </p>
 *
 * @return a boolean reflecting this branch, as described above
 */
@NonCPS def hasPR() {
	try {
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
	} catch(Exception e) {

	}
	return false
}
