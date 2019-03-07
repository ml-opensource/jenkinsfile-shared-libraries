/**
 * Upload files to Amazon's Simple Storage Service (S3).
 * <p>
 *     This defines most of the variables, so callers
 *     can focus on choosing the right profileName and
 *     bucket.
 * </p>
 * <p>
 *     NB: This method is currently a wrapper around the
 *     <a href="https://github.com/jenkinsci/pipeline-aws-plugin/blob/362499cf76270ba064da0c74256fd30c76347205/README.md#s3upload">
 *         Pipeline Plugin's s3Upload feature</a>.
 * </p>
 *
 * @param config use this to indicate what to upload, and
 * where precisely it should be uploaded
 * @return nothing
 */
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