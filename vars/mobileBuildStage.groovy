def call(Closure body) {
	stage("Build") {
		body()
		archiveArtifacts '**/archive/*.ipa, **/output/**/*.apk'
		storeFuzzArtifacts()
	}
}