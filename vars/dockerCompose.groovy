def up(String file) {
	stage("Docker Up") {
		bash "docker-compose -f ${file} down"
		bash "docker system prune --all --force"
		slack.qbash "docker-compose -f ${file} up -d --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
		bash "docker system prune --all --force"
	}
}