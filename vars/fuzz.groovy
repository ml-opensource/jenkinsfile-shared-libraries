import groovy.json.JsonSlurperClassic
import java.lang.InterruptedException
import groovy.json.JsonOutput
import java.util.Optional
import hudson.tasks.junit.TestResultAction
import hudson.model.Actionable
import hudson.tasks.junit.CaseResult
import com.fuzz.artifactstore.ArtifactStoreAction
import com.fuzz.artifactstore.ArtifactStore
import hudson.plugins.cobertura.CoberturaBuildAction
import hudson.plugins.cobertura.targets.CoverageMetric

def sendSlackError(Exception e, String message) {
}

def slackShell(command) {
	try {
		sh command	
	} catch (Exception e) {
		def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
		sendSlackError(e, "${slackBuildNode}Failed to ${command}")
		throw e
	}
}

def slackScriptWrapper(command, errorMessage) {
	try {
		script command	
	} catch (Exception e) {
		def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
		sendSlackError(e, "${slackBuildNode}${errorMessage}")
		throw e
	}
}

def standardReportArchives() {
	try {
		checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*lint.xml', unHealthy: ''
		pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*pmd.xml', unHealthy: ''
		dry canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*cpd.xml', unHealthy: ''
		openTasks canComputeNew: false, defaultEncoding: '', excludePattern: '**/Libraries/**, **/Pods/**, **/*.framework/**, **/Xcode.app/**, **/build/**', healthy: '', high: 'FIXME,shit,fuck,suck', ignoreCase: true, low: 'deprecated', normal: 'TODO', pattern: '**/*.swift, **/*.java, **/*.m, **/*.h, **/*.c', unHealthy: ''
		sloccountPublish encoding: '', pattern: '**/*cloc.xml'
	} catch (Exception e) {
		//Silent Error
	}
}

def reportResultsAndCoverage() {
	try {
		junit allowEmptyResults: true, testResults: '**/*.junit'
		cobertura autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/*cobertura.xml', conditionalCoverageTargets: '70, 0, 0', failNoReports: false, failUnhealthy: false, failUnstable: false, lineCoverageTargets: '80, 0, 0', maxNumberOfBuilds: 0, methodCoverageTargets: '80, 0, 0', onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false							
	} catch (Exception e) {
		//Silent Error
	}
}

def iosPipeline(node, inScript) {
	node(node) {
		ansiColor('xterm') {
			withEnv(['LC_ALL=en_US.UTF-8']) {
				script inScript
			}
		}
	}
}

def archiveAppForTesting(appPath) {
	zip archive: true, dir: "${appPath}", glob: '*.app/**/*', zipFile: "app.zip"
}