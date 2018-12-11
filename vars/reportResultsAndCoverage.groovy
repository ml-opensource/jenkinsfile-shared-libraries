def call(Closure body = null) {
	try {
		//Test Reporting
		junit allowEmptyResults: true, testResults: '**/*.junit, **/junit-reports/*.xml, **/test-results/**/*.xml, **/TEST*.xml'
	} catch (Exception e) {
		throw e
	}
	try {
		//Java and Swift
		cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/*cobertura.xml', conditionalCoverageTargets: '70, 0, 0', failNoReports: false, failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false							
	} catch (Exception e) {
	}

	try {
		//Rcov	
		step([$class: 'RcovPublisher', targets: []])						
	} catch (Exception e) {
	}
}