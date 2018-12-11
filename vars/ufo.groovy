def install() {
	bash "go get -u github.com/fuzz-productions/ufo"
}

def deploy(String cluster = 'dev', String config = 'default') {
	if (config == 'default') {
		slack.bash "ufo deploy --cluster ${cluster}"
	} else {
		slack.bash "ufo deploy --cluster ${cluster} --config ${config}"
	}
}