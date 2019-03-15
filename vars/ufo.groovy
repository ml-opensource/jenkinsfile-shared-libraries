def install() {
	// If the ufo installation should encounter an error, this bit of shell will retry twice more before failing
	bash '''
	for i in 2 1 0
	do
		if go get -u github.com/fuzz-productions/ufo
		then
			exit 0
		fi

		echo "ERROR: problem installing ufo; $i more installation attempt(s) will be made"
		sleep 30
	done
	'''

}

def deploy(String cluster = 'dev', String config = 'default') {
	if (config == 'default') {
		slack.qbash "ufo deploy --cluster ${cluster} --timeout 10"
	} else {
		slack.qbash "ufo deploy --cluster ${cluster} --config ${config} --timeout 10"
	}
}