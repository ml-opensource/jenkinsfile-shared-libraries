def clone(String gitUrl) {
	folderName = gitUrl.split('/')[1].replace(".git","")
	sh "rm -rf ${folderName} || echo 'Done'"
	sh "git clone ${gitUrl}"
}

def mirror(String mirrorURL) {
	if (github.isPR()) {}
		sh "git checkout ${env.CHANGE_BRANCH}"
	} else {
		sh "git checkout ${env.BRANCH_NAME}"
	}
	sh "git remote add github ${mirrorURL}"
	if (github.isPR()) {}
		sh "git push github ${env.CHANGE_BRANCH}"
	} else {
		sh "git push github ${env.BRANCH_NAME}"
	}
}