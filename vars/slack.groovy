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
import groovy.transform.Field

def getSlackChannel() {
    echo "${env.SLACK_CHANNEL}"
    if (env.SLACK_CHANNEL) {
        return env.SLACK_CHANNEL
    } else {
        return "jenkins_notifications"
    }
}

def getLastSuccessfulCommit() {
  def lastSuccessfulHash = null
  def lastSuccessfulBuild = currentBuild.rawBuild.getPreviousSuccessfulBuild()
  if ( lastSuccessfulBuild ) {
    lastSuccessfulHash = commitHashForBuild( lastSuccessfulBuild )
  } else {
    lastSuccessfulBuild = currentBuild.rawBuild.getPreviousBuild() 
    if (lastSuccessfulBuild) {
        lastSuccessfulHash = commitHashForBuild( lastSuccessfulBuild )    
    }   
  }
  return lastSuccessfulHash
}

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
    return gituri.replace(".git","").replace("git@github.com:","https://github.com/")
}

def getCurrentCommitLink() {
    def currentCommit = commitHashForBuild( currentBuild.rawBuild )
    def repoURL = getRepoUrl()
    def commitURL = repoURL + "/commit/"
    def shortHash = currentCommit[0..6]
    return "(<${commitURL}${currentCommit}|${shortHash}>)"
}

def getCommitLog() {
	def lastSuccessfulCommit = getLastSuccessfulCommit()
    def currentCommit = commitHashForBuild( currentBuild.rawBuild )
    def repoURL = getRepoUrl()
    def commitURL = repoURL + "/commit/"
    if (lastSuccessfulCommit) {
        try {
            commits = sh(
                script: "git log --pretty=format:'- %s%b [%an] (<${commitURL}%H|%h>) %n' ${currentCommit} \"^${lastSuccessfulCommit}\"",
                returnStdout: true
            )
            if (commits.equals("")) {
        	    return "No Changes (re-build?)"
            }
        } catch (Throwable t) {
            return "Couldn't get changes (history got changed?)"
        }
        
       	return commits
    }
    return "No Changes (re-build?)"
}


def getArtifacts() { 
    def artifactStores = currentBuild.rawBuild.getAction(ArtifactStoreAction.class)
    def summary = ""
    if (artifactStores != null) {
    	for(ArtifactStore artifact : artifactStores.artifacts) {
			def fileName = artifact.fileName
			def uuid = artifact.UDID
			summary += "<https://builds.fuzzhq.com/install.php?id=${uuid}|${fileName}>\n"
    	}	    
    } else {
        summary = "No Artifacts"
    }
    return summary
}

def getTestSummary() {
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    def summary = ""

    if (testResultAction != null) {
        orgtotal = testResultAction.getTotalCount()
        orgfailed = testResultAction.getFailCount()
        orgskipped = testResultAction.getSkipCount()

        total = orgtotal
        failed = orgfailed
        skipped = orgskipped

        if (env.SLACK_TOTAL) {
            total = env.SLACK_TOTAL.toInteger() - total 
            failed = env.SLACK_FAILED.toInteger() - failed 
            skipped = env.SLACK_SKIPPED.toInteger() - skipped   
        }

        env.SLACK_TOTAL = orgtotal + ""
        env.SLACK_FAILED = orgfailed + ""
        env.SLACK_SKIPPED = orgskipped + ""

        summary = "Passed: " + (total - failed - skipped)
        summary = summary + (", Failed: " + failed)
        summary = summary + (", Skipped: " + skipped)
    } else {
        summary = "No tests found"
    }
    return summary
}

def getCoverageSummary() {
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

def getFailedTests() {
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    if (testResultAction != null) {
    	def failedTestsString = ""
        def failedTests = testResultAction.getFailedTests()

        if (failedTests.size() > 9) {
            failedTests = failedTests.subList(0, 8)
        }

        for(CaseResult cr : failedTests) {
            if (cr.getFullDisplayName().contains("${env.STAGE_NAME} / ")) {
                testDisplayName = cr.getFullDisplayName().replace("${env.STAGE_NAME} / ", "")
                failedTestsString = failedTestsString + "${testDisplayName}:\n${cr.getErrorDetails()}\n\n"
            } else if (!cr.getFullDisplayName().contains(" / ")) {
                failedTestsString = failedTestsString + "${cr.getFullDisplayName()}:\n${cr.getErrorDetails()}\n\n"
            }
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
		sendSlackError(e, "Failed to ${command} in _*Stage ${env.STAGE_NAME}*_")
		throw e
	}
}

def qbash(command) {
    try {
        bash command  
    } catch (Exception e) {
        sendSlackError(e, "Failed to ${command} in _*Stage ${env.STAGE_NAME}*_")
        throw e
    }
}

def wrap(command, errorMessage) {
	try {
		script command	
	} catch (Exception e) {
		sendSlackError(e, "${errorMessage} in _*Stage ${env.STAGE_NAME}*_")
		throw e
	}
}

def jobName() {
	def job = "${env.JOB_NAME}"
	def splits = job.split("/")
	def jobName = splits[1] + "/" + splits[2]
	return jobName
}

def PRMessage() {
	def PRTitle = "${env.CHANGE_TITLE}"
	def PRTarget = "${env.CHANGE_TARGET}"
	def PRAuthor = "${env.CHANGE_AUTHOR}"
	def PRSource = "${env.CHANGE_BRANCH}"
	def PRURL = "${env.CHANGE_URL}"
	return "_*${env.BRANCH_NAME}:*_ ${PRTitle} - _${PRSource} -> ${PRTarget}_ by ${PRAuthor} (<${PRURL}|Open>)\n"
}

def isPR() {
	return github.isPR()
}

def slackHeader() {
	def jobName = jobName()
	def slackHeader = "${jobName} - #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)\n"
	def currentCommitLink = getCurrentCommitLink()
	if (isPR()) {
        slackHeader += "Branch _*${env.CHANGE_BRANCH}*_ ${currentCommitLink}\n"
		slackHeader += PRMessage()
	} else {
		slackHeader += "Branch _*${env.BRANCH_NAME}*_ ${currentCommitLink}\n"
	}
	slackHeader += "Built with _*${env.NODE_NAME}*_\n"
	return slackHeader
}

def sendSlackError(Exception e, String message) {
	if (!(e instanceof InterruptedException)) {
        def logs = currentBuild.rawBuild.getLog(200)
        def logsToPrint = []
        def addToLogs = false
        for(String logString : logs) {
            if (logString) {
                if (logString.contains("fastlane finished with errors")) {
                    addToLogs = true
                } else if (logString.contains("FAILURE: Build failed with an exception.")) {
                    addToLogs = true    
                }
            }
            if (addToLogs) {
                if (logString.contains("from /Users") && logString.contains("gems/fastlane") && logString.contains("lib/fastlane_core")) {
                    addToLogs = false
                } else if (logString.contains("* Exception is:")) {
                    addToLogs = false
                }
            }
            if (addToLogs) {
                logsToPrint.add(logString)
            }
        } 
        logsString = logsToPrint.subList(Math.max(logsToPrint.size() - 20, 0), logsToPrint.size()).join("\n")
		slackSend color: 'danger', channel: slackChannel, message:slackHeader() + message
        slackSend color: 'danger', channel: slackChannel, message:"```${logsString}```"  
	}
}

def buildMessage() {
	def jobName = jobName()
	def slackHeader = slackHeader()
	def slackArtifacts = getArtifacts()
	slackSend color: 'good', channel: slackChannel, message: slackHeader + slackArtifacts
	def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
	slackSend color: 'good', channel: slackChannel, message: commitLogHeader + getCommitLog()
}

def linkMessage(String inURL) {
    def jobName = jobName()
    def slackHeader = slackHeader()
    def slackArtifacts = "${inURL}\n"
    slackSend color: 'good', channel: slackChannel, message: slackHeader + slackArtifacts
    def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
    slackSend color: 'good', channel: slackChannel, message: commitLogHeader + getCommitLog()
}

def testMessage() {
	def slackHeader = slackHeader()
	def failedTest = getFailedTests()
	def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
	def coverageSummary = "_*Code Coverage*_\n" + getCoverageSummary() + "\n"
	def slackTestSummary = testSummary + coverageSummary
	if (failedTest == null) {
		if (testSummary.contains("No tests found")) {
			slackSend color: 'warning', channel: slackChannel, message: slackHeader + slackTestSummary 
		} else {
			slackSend color: 'good', channel: slackChannel, message: slackHeader + slackTestSummary 
		}
	} else {
		slackSend color: 'warning', channel: slackChannel, message: slackHeader + slackTestSummary
		slackSend color: 'warning', channel: slackChannel, message: failedTest
	}
}

def uatMessage() {
    def slackHeader = slackHeader()
    def failedTest = getFailedTests()
    def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
    def reportMessage = "_*Report*_\n" + env.BUILD_URL + "Extent-Reports/" + "\n"
    if (env.TEST_RAIL_ID) {
        def testRailURL = "https://fuzz.testrail.io/index.php?/runs/overview/${env.TEST_RAIL_ID}" 
    }
    def slackTestSummary = testSummary + reportMessage
    if (failedTest == null) {
        if (testSummary.contains("No tests found")) {
            slackSend color: 'warning', channel: slackChannel, message: slackHeader + slackTestSummary 
        } else {
            slackSend color: 'good', channel: slackChannel, message: slackHeader + slackTestSummary 
        }
    } else {
        slackSend color: 'warning', channel: slackChannel, message: slackHeader + slackTestSummary
        slackSend color: 'warning', channel: slackChannel, message: failedTest
    }
}

def echo() {
	def slackHeader = slackHeader()
	slackSend color: 'good', channel: slackChannel, message: slackHeader
}