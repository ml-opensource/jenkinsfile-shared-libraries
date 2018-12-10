def up(String file) {
	stage("Docker Up") {
		volumes = sh(script: "docker volume ls -qf dangling=true", returnStdout: true)
		bash "docker volume rm $(volumes)"
		bash "docker-compose -f ${file} up -d --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
	}
}