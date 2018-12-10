def up(String file) {
	stage("Docker Up") {
		bash "docker-compose -f ${file} down"
		bash "docker volume prune --force"
		bash "docker-compose -f ${file} up -d --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
	}
}