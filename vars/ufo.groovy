def install() {
	bash "go get -u github.com/fuzz-productions/ufo"
}

def deploy(String cluster = 'dev', String config = 'default') {
	if (config == 'default') {
		slack.qbash "ufo deploy --cluster ${cluster} --timeout 10"
	} else {
		slack.qbash "ufo deploy --cluster ${cluster} --config ${config} --timeout 10"
	}
}