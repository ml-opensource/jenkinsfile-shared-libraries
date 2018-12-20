def call(Closure body = null) {
	try {
		//Test Reporting
		junit allowEmptyResults: true, testResults: '**/*.junit, **/junit-reports/*.xml, **/test-results/**/*.xml, **/TEST*.xml, **/*junit.xml'
	} catch (Exception e) {
		throw e
	}
	try {
		//Java and Swift
		cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/*cobertura.xml,**/*cobertura*.xml', conditionalCoverageTargets: '70, 0, 0', failNoReports: false, failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false							
	} catch (Exception e) {
	}

	try {
		//Clover
		step([
    		$class: 'CloverPublisher',
    		cloverReportDir: '~/',
    		cloverReportFileName: 'clover.xml',
    		healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80], // optional, default is: method=70, conditional=80, statement=80
    		unhealthyTarget: [methodCoverage: 50, conditionalCoverage: 50, statementCoverage: 50], // optional, default is none
    		failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]     // optional, default is none
  		])			
	} catch (Exception e) {
	}

	try {
		//Rcov	
		step([$class: 'RcovPublisher', targets: []])						
	} catch (Exception e) {
	}

	if (slack.getTestSummary() == "No tests found") {
		currentBuild.result = "UNSTABLE"
	}
}