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

def sh(command) {
	try {
		sh command	
	} catch (Exception e) {
		def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
		sendSlackError(e, "${slackBuildNode}Failed to ${command}")
		throw e
	}
}

def wrap(command, errorMessage) {
	try {
		script command	
	} catch (Exception e) {
		def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
		sendSlackError(e, "${slackBuildNode}${errorMessage}")
		throw e
	}
}