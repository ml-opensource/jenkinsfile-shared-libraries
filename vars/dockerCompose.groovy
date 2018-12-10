def up(String file) {
	stage("Docker Up") {
		bash "docker-compose -f ${file} up --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
	}
}