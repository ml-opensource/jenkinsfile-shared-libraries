def up(String file) {
	stage("Docker Up") {
		bash "docker-compose -f ${file} down"
		volumes = sh(script: "docker volume ls -qf dangling=true", returnStdout: true)
		if (volumes.trim()) {
			sh "echo ${volumes}"
			bash "docker volume rm ${volumes}"
		}
		bash "docker-compose -f ${file} up -d --build"
	}
}

def down(String file) {
	stage("Docker Down") {
		bash "docker-compose -f ${file} down"
	}
}