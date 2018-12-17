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
	try {
		sh "git remote add github ${mirrorURL}"
	} catch(Exception e) {

	}
	if (credential) {
		withCredentials([usernamePassword(credentialsId: credential, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
			sh "git config user.name ${env.GIT_USERNAME}"
			sh "git config user.password ${env.GIT_PASSWORD}"
			sh "git push github ${branch}"
		}
	} else {
		sh "git push github ${branch}"
	}
}