/**
 * Clone a Git repository into a local folder.
 * <p>
 *     C.f. <a href="https://git-scm.com/book/en/v2/Git-Basics-Getting-a-Git-Repository">
 *         the Git SCM book
 *     </a>.
 * </p>
 *
 * @param gitUrl a <code>git://</code> or <code>ssh://</code> url
 * @param branch the name of a Git branch in that repository. Optional.
 * @return nothing
 */
def clone(String gitUrl, String branch = '') {
	folderName = gitUrl.split('/')[1].replace(".git","")
	sh "rm -rf ${folderName} || echo 'Done'"
	if (branch == '') {
		sh "git clone ${gitUrl}"
	} else {
		sh "git clone ${gitUrl} -b ${branch}"
	}
}

/**
 * This has very little to do with actual Git mirrors.
 * <p>
 *     Update the local directory with whatever's on 'origin', then
 *     push that content to 'mirrorUrl'. Should only affect the current
 *     Git branch.
 * </p>
 *
 * @param mirrorURL  some remote git repository that ought to be updated
 * @param credential id of an AWS profile with git username/password overrides. Optional.
 * @return nothing
 */
def mirror(String mirrorURL, String credential = "") {
	branch = env.BRANCH_NAME
	if (github.isPR()) {
		branch = env.CHANGE_BRANCH
	}
	sh "git checkout ${branch}"
	sh "git pull origin ${branch}"
	if (credential) {
		withCredentials([usernamePassword(credentialsId: credential, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
			authURL = mirrorURL.replace("https://","https://${GIT_USERNAME}:${GIT_PASSWORD}@")
			//sh "git remote add github ${authURL}"
			sh "git push ${authURL} ${branch}"
		}
	} else {
		//sh "git remote add github ${mirrorURL}"
		sh "git push ${mirrorURL} ${branch}"
	}
}

/**
 * Derive the first seven characters of the current commit of the code which is being built.
 *
 * @return that 'short' Git hash, in the form of a String
 */
def shortHash() {
	def scmAction = currentBuild.rawBuild?.actions.find { action -> action instanceof jenkins.scm.api.SCMRevisionAction }
  	def revision = scmAction?.revision
  	if (revision instanceof org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision) {
  		return revision?.pullHash[0..6]
  	}
  	return revision?.hash[0..6]
}