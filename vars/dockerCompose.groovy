def up(String file) {
	stage("Docker Up") {
		bash 'docker volume rm $(docker volume ls -qf dangling=true)'
		bash "docker-compose -f ${file} up -d --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
	}
}