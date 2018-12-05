def call(Map config) {
	regionName = config.get('selectedRegion', 'us-east-1')
	s3Upload consoleLogLevel: 'INFO', 
			dontWaitForConcurrentBuildCompletion: false, 
			entries: [
				[
					bucket: "${config.bucket}", 
					excludedFile: '', 
					flatten: false, 
					gzipFiles: false, 
					keepForever: false, 
					managedArtifacts: false, 
					noUploadOnFailure: true, 
					selectedRegion: "${regionName}", 
					showDirectlyInBrowser: false, 
					sourceFile: "${config.sourceFile}", 
					storageClass: 'STANDARD', 
					uploadFromSlave: true, 
					useServerSideEncryption: false
				]
			], 
			pluginFailureResultConstraint: 'FAILURE', 
			profileName: "${config.profileName}", 
			userMetadata: []
}