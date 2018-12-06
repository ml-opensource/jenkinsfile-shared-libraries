def clone(String gitUrl) {
	folderName = gitUrl.split('/')[1].replace(".git","")
	sh "rm -rf ${folderName} || echo 'Done'"
	sh "git clone ${gitUrl}"
}