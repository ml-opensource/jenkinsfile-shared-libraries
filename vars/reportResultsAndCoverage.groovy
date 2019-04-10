/**
 * Check test coverage of the current project.
 * <p>
 *     We first run <a href="https://junit.org/junit5/">JUnit</a> to build
 *     an idea of what tests exist. Then, we derive a couple coverage stats:
 *     <ul>
 *         <li><a href="https://cobertura.github.io/cobertura/">Cobertura</a></li>
 *         <li><a href="https://openclover.org/">OpenClover</a></li>
 *         <li><a href="https://github.com/relevance/rcov">RCov</a></li>
 *     </ul>
 *     See {@link uatStage} for assistance with integration tests and with
 *     tests stored in external repositories. For other code quality metrics,
 *     try {@link standardReportArchives}.
 * </p>
 * <p>
 *     This method suppresses all exceptions, but it <em>will</em> set
 *     build result status to UNSTABLE if a test fails.
 * </p>
 *
 * @param body this parameter is ignored, and does not run
 * @return nothing
 */
def call(Closure body = null) {
	try {
		//Test Reporting
		junit allowEmptyResults: true, testResults: '**/*.junit, **/junit-reports/*.xml, **/test-results/**/*.xml, **/TEST*.xml, **/*junit.xml, **/testng-results.xml'
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
    		cloverReportDir: 'coverage',
    		cloverReportFileName: 'clover.xml',
    		healthyTarget: [methodCoverage: 70, conditionalCoverage: 80, statementCoverage: 80], // optional, default is: method=70, conditional=80, statement=80
    		unhealthyTarget: [methodCoverage: 50, conditionalCoverage: 50, statementCoverage: 50], // optional, default is none
    		failingTarget: [methodCoverage: 0, conditionalCoverage: 0, statementCoverage: 0]     // optional, default is none
  		])			
	} catch (Exception e) {
	}

	try {
		//Clover
		step([
    		$class: 'CloverPublisher',
    		cloverReportDir: './',
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

	if (!slack.hasTest()) {
		currentBuild.result = "UNSTABLE"
	}
}
