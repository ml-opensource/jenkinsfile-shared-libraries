def clone(String gitUrl) {
	folderName = gitUrl.split('/')[1].replace(".git","")
	sh "rm -rf ${folderName} || echo 'Done'"
	sh "git clone ${gitUrl}"
}

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

def shortHash() {
	def scmAction = currentBuild.rawBuild?.actions.find { action -> action instanceof jenkins.scm.api.SCMRevisionAction }
  	def revision = scmAction?.revision
  	if (revision instanceof org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision) {
  		return revision?.pullHash[0..6]
  	}
  	return revision?.hash[0..6]
}