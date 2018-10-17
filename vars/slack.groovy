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

def slackChannel = "jenkins_notifications"

def getLastSuccessfulCommit() {
  def lastSuccessfulHash = null
  def lastSuccessfulBuild = currentBuild.rawBuild.getPreviousSuccessfulBuild()
  if ( lastSuccessfulBuild ) {
    lastSuccessfulHash = commitHashForBuild( lastSuccessfulBuild )
  }
  return lastSuccessfulHash
}

@NonCPS
def commitHashForBuild( build ) {
  def scmAction = build?.actions.find { action -> action instanceof jenkins.scm.api.SCMRevisionAction }
  def revision = scmAction?.revision
  if (revision instanceof org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision) {
  	return revision?.pullHash
  }
  return revision?.hash
}

def getRepoUrl() {
	def gituri = scm.repositories[0].uris[0].toASCIIString()
    return gituri.replace(".git","").replace("git@","")
}

def getCurrentCommitLink() {
    def currentCommit = commitHashForBuild( currentBuild.rawBuild )
    def repoURL = getRepoUrl()
    def parts = repoURL.split(":")
    def baseURL = "https://" + parts[0] + "/"
    def commitURL = baseURL + parts[1] + "/commit/"
    def shortHash = currentCommit[0..6]
    return "(<${commitURL}${currentCommit}|${shortHash}>)"
}

def getCommitLog() {
	def lastSuccessfulCommit = getLastSuccessfulCommit()
    def currentCommit = commitHashForBuild( currentBuild.rawBuild )
    def repoURL = getRepoUrl()
    def parts = repoURL.split(":")
    def baseURL = "https://" + parts[0] + "/"
    def commitURL = baseURL + parts[1] + "/commit/"
    if (lastSuccessfulCommit) {
        commits = sh(
          script: "git log --pretty=format:'- %s%b [%an] (<${commitURL}%H|%h>) %n' ${currentCommit} \"^${lastSuccessfulCommit}\"",
          returnStdout: true
        )
        if (commits.equals("")) {
        	return "No Changes (re-build?)"
        }
       	return commits
    }
    return "No Changes (re-build?)"
}


def getArtifacts( branch ) { 
    def artifactStores = currentBuild.rawBuild.getAction(ArtifactStoreAction.class)
    def summary = ""
    if (artifactStores != null) {
    	for(ArtifactStore artifact : artifactStores.artifacts) {
			def fileName = artifact.fileName
			def uuid = artifact.UDID
			if (fileName.contains("_mr_") && branch.contains("_mr_")) {
				summary += "<https://builds.fuzzhq.com/install.php?id=${uuid}|${fileName}>\n"
			} else if (!branch.contains("_mr_") && !fileName.contains("_mr_")) {		
    			summary += "<https://builds.fuzzhq.com/install.php?id=${uuid}|${fileName}>\n"
			}
    	}	    
    } else {
        summary = "No Artifacts"
    }
    return summary
}

def getTestSummary = { ->
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    def summary = ""

    if (testResultAction != null) {
        total = testResultAction.getTotalCount()
        failed = testResultAction.getFailCount()
        skipped = testResultAction.getSkipCount()

        summary = "Passed: " + (total - failed - skipped)
        summary = summary + (", Failed: " + failed)
        summary = summary + (", Skipped: " + skipped)
    } else {
        summary = "No tests found"
    }
    return summary
}

def getCoverageSummary = { ->
    def coverageAction = currentBuild.rawBuild.getAction(CoberturaBuildAction.class)
    def summary = ""

    if (coverageAction != null) {
        def lineData = coverageAction.getResult().getCoverage(CoverageMetric.LINE)
        if (lineData != null) {
        	summary = "Lines Covered: " + lineData.getPercentage() + "%"
        } else {
        	summary = "No Coverage Data"
        }
    } else {
        summary = "No Coverage Data"
    }
    return summary
}

def getFailedTests = { ->
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    if (testResultAction != null) {
    	def failedTestsString = ""
        def failedTests = testResultAction.getFailedTests()

        if (failedTests.size() > 9) {
            failedTests = failedTests.subList(0, 8)
        }

        for(CaseResult cr : failedTests) {
            failedTestsString = failedTestsString + "${cr.getFullDisplayName()}:\n${cr.getErrorDetails()}\n\n"
        }
        if (failedTestsString.equals("")) {
        	return null;
        } else {
        	return "```" + failedTestsString + "```"
        }
    } else {
    	return null
    }
}

def qsh(command) {
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

def jobName() {
	def job = "${env.JOB_NAME}"
	def splits = job.split("/")
	return splits[1] + "/" + splits[2]
}

def PRMessage() {
	def PRTitle = "${env.CHANGE_TITLE}"
	def PRTarget = "${env.CHANGE_TARGET}"
	def PRAuthor = "${env.CHANGE_AUTHOR}"
	def PRURL = "${env.CHANGE_URL}"
	return "${PRTitle} - ${PRTarget} by ${PRAuthor} (<${PRURL}|Open>)\n"
}

def isPR() {
	def PRTitle = "${env.CHANGE_TITLE}"
	return !PRTitle.trim().equals("")
}

def slackHeader() {
	def jobName = jobName()
	def slackHeader = "${jobName} - #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)\n"
	def currentCommitLink = getCurrentCommitLink()
	slackHeader += "Branch _*${env.BRANCH_NAME}*_ ${currentCommitLink}\n"
	if (isPR()) {
		slackHeader += PRMessage()
	}
	return slackHeader
}

def sendSlackError(Exception e, String message) {
	if (!(e instanceof InterruptedException)) {
		slackSend color: 'danger', channel: slackChannel, message:slackHeader() +  + message.replace("@here", "")
	}
}

def buildMessage() {
	def jobName = jobName()
	def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
	def slackHeader = slackHeader()
	def slackSuccessHeader = "${slackHeader}${slackBuildNode}"
	def slackArtifacts = getArtifacts(source)
	slackSend color: 'good', channel: slackChannel, message: slackSuccessHeader + slackArtifacts
	def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
	slackSend color: 'good', channel: slackChannel, message: commitLogHeader + getCommitLog()
}

def testMessage() {
	def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
	def slackHeader = slackHeader()
	def slackSuccessHeader = "${slackHeader}${slackBuildNode}"
	def failedTest = getFailedTests()
	def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
	def coverageSummary = "_*Code Coverage*_\n" + getCoverageSummary() + "\n"
	def slackTestSummary = testSummary + coverageSummary
	if (failedTest == null) {
		if (testSummary.contains("No tests found")) {
			slackSend color: 'warning', channel: slackChannel, message: slackSuccessHeader + slackTestSummary 
		} else {
			slackSend color: 'good', channel: slackChannel, message: slackSuccessHeader + slackTestSummary 
		}
	} else {
		slackSend color: 'warning', channel: slackChannel, message: slackSuccessHeader + slackTestSummary
		slackSend color: 'warning', channel: slackChannel, message: failedTest
	}
}

def echo() {
	def jobName = jobName()
	def slackBuildNode = "Built with _*${env.NODE_NAME}*_\n"
	def slackHeader = slackHeader()
	def slackSuccessHeader = "${slackHeader}${slackBuildNode}"
	slackSend color: 'good', channel: slackChannel, message: slackSuccessHeader
}