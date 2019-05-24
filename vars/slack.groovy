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
import hudson.plugins.clover.CloverBuildAction
import hudson.plugins.clover.results.ProjectCoverage
import hudson.plugins.clover.Ratio
import groovy.transform.Field

/**
 * Get the name of the current build's Slack channel.
 * <p>
 *     Not all projects define an associated channel. In such a
 *     case, this method will return the constant
 *     "jenkins_notifications".
 * </p>
 *
 * @return either env.SLACK_CHANNEL (if present) or "jenkins_notifications"
 */
def getSlackChannel() {
    if (env.SLACK_CHANNEL) {
        return env.SLACK_CHANNEL
    } else {
        return "jenkins_notifications"
    }
}

/**
 * Internal method, intended for use by {@link slack#getCommitLog}.
 * <p>
 *     Determine what would be the best reference point for the
 *     current build.
 * </p>
 * <p>
 *     This method makes implicit use of a concept which I refer
 *     to as a 'pipeline branch'. This is
 *     <ul>
 *         <li>Informally: a Git branch or pull request</li>
 *         <li>Formally: a temporally-ordered list of builds with similar properties.</li>
 *     </ul>
 *     Unlike regular VCS branches, there's nothing stopping you from
 *     building multiple builds in a pipeline branch from the exact same
 *     code.
 * </p>
 *
 * @return the VCS commit hash of that build, or null if none could be found
 */
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

/**
 * Internal method to determine what code was used when running <code>build</code>.
 * <p>
 *     Note that it is fully possible to run a build without any code.
 *     If the parameter here represents one of those kinds of builds,
 *     then we'll return null.
 * </p>
 *
 * @param build probably a <a href="https://javadoc.jenkins-ci.org/hudson/model/Run.html">
 *     "Run"</a> or a "Job"
 * @return a VCS commit hash, or null if none could be found
 */
def commitHashForBuild( build ) {
  def scmAction = build?.actions.find { action -> action instanceof jenkins.scm.api.SCMRevisionAction }
  def revision = scmAction?.revision
  if (revision instanceof org.jenkinsci.plugins.github_branch_source.PullRequestSCMRevision) {
  	return revision?.pullHash
  }
  return revision?.hash
}

/**
 * Internal method to derive the url of the current build's GitHub repository.
 * <p>
 *     The returned string should be of the format
 *     <pre>https://github.com/&lt;group name&gt;/&lt;project name&gt;</pre>
 *     Here are two (fake) return values so you can get a feel for the concept:
 *     <pre>
 *     https://github.com/fast/veryfast
 *     https://github.com/fuzz-productions/marsupial-ios
 *     </pre>
 * </p>
 *
 * @return a website url, as described above
 */
def getRepoUrl() {
	def gituri = scm.repositories[0].uris[0].toASCIIString()
    return gituri.replace(".git","").replace("git@github.com:","https://github.com/")
}

/**
 * Internal method to create an HTML link to the code used for this build.
 * <p>
 *     Slack has an idiosyncratic formatter; if you're familiar
 *     with MediaWiki (that's the format used on Wikipedia) it's not
 *     <em>too</em> different.
 * </p>
 *
 * @return a url with shortened display name, suitable for use in a slack message
 * @see slack#getRepoUrl
 * @see slack#commitHashForBuild
 */
def getCurrentCommitLink() {
    def currentCommit = commitHashForBuild( currentBuild.rawBuild )
    def repoURL = getRepoUrl()
    def commitURL = repoURL + "/commit/"
    def shortHash = currentCommit[0..6]
    return "(<${commitURL}${currentCommit}|${shortHash}>)"
}

/**
 * Create a newline-separated list of the last however-many changes since
 * {@link slack#getLastSuccessfulCommit() an arbitrary reference commit}.
 * <p>
 *     Note that there are often a lot of commits - sometimes over 30, even.
 *     For that reason please be careful about changing the 'git log' call
 *     in this method.
 * </p>
 * <p>
 *     We don't always return a long string. There are short status
 *     messages for common scenarios, like
 *     <ul>
 *         <li>there is no reference commit available</li>
 *         <li>the reference commit is the same as that used for the current build</li>
 *         <li>the 'git log' command fails with an error (we have our own message for that)</li>
 *     </ul>
 * </p>
 *
 * @return a string containing the list of commits, or a message
 */
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


/**
 * Create a newline-separated list of all of the installable artifacts on the
 * current build.
 * <p>
 *     Each ArtifactStore has two pieces of metadata which we care about in this
 *     function:
 *     <ol>
 *         <li>a user-facing name   ('fileName')</li>
 *         <li>a unique identifier  ('UDID')</li>
 *     </ol>
 *     With these, we build urls of a form analogous to
 *     <pre>https://builds.fuzzhq.com/install.php?id=33334444-5555-6666-7777-aaaabbbbcccc</pre>
 *     (the name becomes the 'text' of the link).
 * </p>
 * <p>
 *     If no artifacts exist on the current build, this will return a nice little
 *     message indicating as much.
 * </p>
 * <p>
 *     C.f. external library for <code>com.fuzz.artifactstore.ArtifactStore</code>.
 * </p>
 *
 * @return a string containing the list of artifacts, or a message
 */
def getArtifacts() { 
    def summary = ""
    try {
	    def artifactStores = currentBuild.rawBuild.getAction(ArtifactStoreAction.class)
	    if (artifactStores != null) {
		    for(ArtifactStore artifact : artifactStores.artifacts) {
			    def fileName = artifact.fileName
			    def uuid = artifact.UDID
			    summary += "<https://builds.fuzzhq.com/install.php?id=${uuid}|${fileName}>\n"
		    }	    
	    } else {
		    summary = "No Artifacts"
	    }
    } catch (Throwable t) {
	    println "Does not have Artifact Store Installed" 
	    summary = "No Artifacts"
    }
    return summary
}

/**
 * This identifies whether at least one <code>TestResultAction</code> has been
 * registered with the current build.
 * <p>
 *     It's the responsibility of individual build scripts to register and run
 *     JUnit tests correctly. This method will return false up until the JUnit
 *     work actually finishes.
 * </p>
 *
 * @return true if at least one JUnit test is registered, false otherwise
 * @see slack#getTestSummary
 * @see slack#getFailedTests
 */
def hasTest() {
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    return testResultAction != null
}

/**
 * Internal method for {@link slack#testMessage} and {@link slack#uatMessage}.
 * <p>
 *     If the current build already {@link slack#hasTest has a test result},
 *     then you can use this to build a short human-readable summary of that
 *     result. Otherwise this'll just return "No tests found".
 * </p>
 * <br/>
 * <p>
 *     <b>If present, the summary will contain the following:</b>
 *     <table>
 *         <tr><th>Concept</th><th>Can be retrieved later by checking</th></tr>
 *         <tr><td>number of tests that passed</td><td><code>env.SLACK_TEST_TOTAL</code></td></tr>
 *         <tr><td>number of tests that failed</td><td><code>env.SLACK_TEST_FAILED</code></td></tr>
 *         <tr><td>number of tests that were skipped or ignored</td><td><code>env.SLACK_TEST_SKIPPED</code></td></tr>
 *     </table>
 *     <br/>
 *     This method acts differently if those 'env' values have already been defined.
 * </p>
 *
 * @return a summary as described above, or just a message
 * @see slack#getCoverageSummary
 * @see slack#getFailedTests
 */
def getTestSummary() {
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    def summary = ""

    if (testResultAction != null) {
        orgtotal = testResultAction.getTotalCount()
        orgfailed = testResultAction.getFailCount()
        orgskipped = testResultAction.getSkipCount()
	    
        total = testResultAction.getTotalCount()
        failed = testResultAction.getFailCount()
        skipped = testResultAction.getSkipCount()

        if (env.SLACK_TEST_TOTAL && env.SLACK_TEST_TOTAL.toInteger() > 0) {
            total = orgtotal - env.SLACK_TEST_TOTAL.toInteger()
            failed = orgfailed - env.SLACK_TEST_FAILED.toInteger() 
            skipped = orgskipped - env.SLACK_TEST_SKIPPED.toInteger()   
        }

        env.SLACK_TEST_TOTAL="${orgtotal}"
        env.SLACK_TEST_FAILED="${orgfailed}"
        env.SLACK_TEST_SKIPPED="${orgskipped}"
	    
        summary = "Passed: " + (total - failed - skipped)
        summary = summary + (", Failed: " + failed)
        summary = summary + (", Skipped: " + skipped)
    } else {
        summary = "No tests found"
    }
    return summary
}

/**
 * Internal method to get the total code coverage as a percentage.
 * <p>
 *     A typical return value would be something like
 *     <pre>"Lines Covered: 31%"</pre>
 * </p>
 * <p>
 *     Despite the use of three plugins in {@link reportResultsAndCoverage#call},
 *     this method only trusts Cobertura's report. If that's missing, we just
 *     return "No Coverage Data" here.
 * </p>
 *
 * @return a percentage of covered code, or a message
 * @see slack#getTestSummary
 */
def getCoverageSummary() {
    def coverageAction = currentBuild.rawBuild.getAction(CoberturaBuildAction.class)
    def cloverCoverageAction = currentBuild.rawBuild.getAction(CloverBuildAction.class)
    def summary = ""

    if (coverageAction != null) {
        def lineData = coverageAction.getResult().getCoverage(CoverageMetric.LINE)
        if (lineData != null) {
        	summary = "Lines Covered: " + lineData.getPercentage() + "%"
        } else {
        	summary = "No Coverage Data"
        }
    } else if (cloverCoverageAction != null) {
        def coverageData = cloverCoverageAction.getResult()
        if (coverageData != null) {
            summary = "Lines Covered: " + coverageData.getStatementCoverage().getPercentageStr()
        }
    } else {
        summary = "No Coverage Data"
    }
    return summary
}

/**
 * Internal method to build a quick summary of test failures.
 * <p>
 *     These should be concise and expressive. For each, expect no more than
 *     just the name of the test, the type of failure, and the error message
 *     seen (if any). This is <em>NOT</em> the place for multi-line stacktraces.
 * </p>
 * <p>
 *     NB: The returned string is only intended for display with a monospace
 *     font. To that end, it currently starts and ends with <code>```</code>
 *     (triple backticks), which is how you tell Slack to drop formatting.
 * </p>
 *
 * @return a summary of the test failures, or null if there are none
 * @see slack#hasTest
 * @see slack#getTestSummary
 * @see slack#sendSlackError
 */
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

/**
 * Wrapper around '<code>sh</code> command'.
 * <p>
 *     If the command fails, this method will indicate so via {@link slack#sendSlackError}.
 *     The content of the error itself is not and <em>should not be</em> sent to Slack.
 * </p>
 *
 * @param command arbitrary commands. Coreutils should be available
 * @return nothing
 * @see bash#call
 * @see slack#qbash
 */
def qsh(command) {
	try {
		sh command	
	} catch (Exception e) {
		sendSlackError(e, "Failed to ${command} in _*Stage ${env.STAGE_NAME}*_")
		throw e
	}
}

/**
 * Wrapper around '{@link bash#call bash} command'.
 * <p>
 *     If the command fails, this method will indicate so via {@link slack#sendSlackError}.
 *     The content of the error itself is not and <em>should not be</em> sent to Slack.
 * </p>
 *
 * @param command arbitrary commands. Coreutils should be available
 * @return nothing
 * @see bash#call
 * @see slack#qsh
 */
def qbash(command) {
    try {
        bash command  
    } catch (Exception e) {
        sendSlackError(e, "Failed to ${command} in _*Stage ${env.STAGE_NAME}*_")
        throw e
    }
}

/**
 * Wrapper around '<code>script</code> command'.
 * <p>
 *     If the command fails, this method will indicate so via {@link slack#sendSlackError}.
 *     The content of the error itself is not and <em>should not be</em> sent to Slack.
 * </p>
 *
 * @param command      ??? the syntax for this is not well-defined
 * @param errorMessage what to say in the {@link slack#sendSlackError slack error message}
 * @return nothing
 */
def wrap(command, errorMessage) {
	try {
		script command	
	} catch (Exception e) {
		sendSlackError(e, "${errorMessage} in _*Stage ${env.STAGE_NAME}*_")
		throw e
	}
}

/**
 * Retrieve the name of the current Job.
 * <p>
 *     If the env.JOB_NAME string has two or more <code>/</code> (slashes), then this
 *     will return the second and third slash-separated pieces of that string. That
 *     slightly-unusual logic makes GitHub-derived Job names much prettier.
 * </p>
 *
 * @return a user-friendly name for the current job
 */
def jobName() {
	def job = "${env.JOB_NAME}"
	def splits = job.split("/")
    if (splits.length > 1) {
	   def jobName = splits[splits.length - 2] + "/" + splits[splits.length - 1]
	   return jobName
    } else {
        return job;
    }
}

/**
 * Internal method to describe a Pull Request.
 * <p>
 *     Callers are responsible for ensuring that all the environment's
 *     'env.CHANGE_*' attributes are correct before calling this.
 * </p>
 * <p>
 *     Much like e.g. {@link slack#getTestSummary}, the returned string is
 *     already formatted with italic and bold markup.
 * </p>
 *
 * @return a single-line message containing all sorts of interesting info
 * @see prettyNode#hasPR
 */
def PRMessage() {
	def PRTitle = "${env.CHANGE_TITLE}"
	def PRTarget = "${env.CHANGE_TARGET}"
	def PRAuthor = "${env.CHANGE_AUTHOR}"
	def PRSource = "${env.CHANGE_BRANCH}"
	def PRURL = "${env.CHANGE_URL}"
	return "_*${env.BRANCH_NAME}:*_ ${PRTitle} - _${PRSource} -> ${PRTarget}_ by ${PRAuthor} (<${PRURL}|Open>)\n"
}

/**
 * This is currently an alias to {@link github#isPR}.
 *
 * @return true if the current build is probably a Pull Request, false otherwise
 */
def isPR() {
	return github.isPR()
}

/**
 * Internal method to create a nice-looking textual summary of the current build.
 * <p>
 *     Expect useful metadata here, such as the build number, the branch name,
 *     which node was used to run the build, whether this is a Pull Request,
 *     that sort of thing. This doesn't include a list of artifacts or the test
 *     status, as those can sometimes take up a lot of space.
 * </p>
 *
 * @return a newline-separated string, ready for posting to Slack
 * @see slack#getTestSummary
 * @see slack#PRMessage
 * @see slack#echo
 */
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

/**
 * If something throws an Exception and Slack ought to know about it, call this.
 * <p>
 *     The message will be sent directly to Slack along with a
 *     {@link slack#slackHeader standard header}.
 * </p>
 * </br>
 * <p>
 *     In addition, we proactively check the log here for the most common
 *     {@link fastlane} and {@link gradlew} build errors. If we find any, they'll
 *     be attached to the slack call with up to 20 (?) lines of context.
 * </p>
 *
 * @param e       the exception object that instigated this method call. Can be null.
 * @param message the error message to send. Should not be null
 * @return nothing
 */
def sendSlackError(Exception e, String message) {
    def errorMessage = e.toString()
    if (!(e instanceof InterruptedException) && env.SLACK_CHANNEL_NOTIFIED != "true" && !errorMessage.contains("Queue task was cancelled")) {
        env.SLACK_CHANNEL_NOTIFIED = "true"
        def logs = currentBuild.rawBuild.getLog(200).reverse()
        def logsToPrint = []
        def addToLogs = true
        for(String logString : logs) {
            if (logString.contains("from /Users")) { //iOS Ruby Exceptions
            } else if (logString.contains("/lib/rails/") || logString.contains("/.rvm/gems/ruby")) { //iOS Ruby Exceptions
            } else if(logString.contains("fastlane finished with errors")) {
                if (logsToPrint.size() > 0) {
                    addToLogs = false    
                }
            } else if(logString.contains("[Pipeline]")) { //Jenkins Pipeline Info
                if (logsToPrint.size() > 0) {
                    addToLogs = false    
                }
            } else if (logString.contains("at ") && (logString.contains(".java") || logString.contains(".kt") || logString.contains(".groovy"))) { //Gradle Exceptions
            } else if (logString.contains("FAILURE: Build failed with an exception")) {
                if (logsToPrint.size() > 0) {
                    addToLogs = false    
                }
            } else {
                if (addToLogs) {
                    logsToPrint.add(logString)
                }
            }
        } 
        logsToPrint = logsToPrint.reverse()
        logsString = logsToPrint.subList(Math.max(logsToPrint.size() - 20, 0), logsToPrint.size()).join("\n")
        slackSend color: 'danger', channel: slackChannel, message:slackHeader() + message
        slackSend color: 'danger', channel: slackChannel, message:"```${logsString}```"

        if (!errorMessage.contains("script returned exit code 1")) {
           slackSend color: 'danger', channel: "jenkins_notifications", message:slackHeader() + "${e}"   
        }
    }
}

/**
 * Send a 'Build Complete!' Slack message including
 * <ul>
 *     <li>{@link slack#slackHeader Standard header}</li>
 *     <li>{@link slack#getArtifacts List of artifacts on current build}</li>
 *     <li>{@link slack#jobName Job name}</li>
 *     <li>Build number</li>
 *     <li>Link to VCS changelog</li>
 *     <li>{@link slack#getCommitLog List of commit messages}</li>
 * </ul>
 *
 * @return a newline-separated string, as described above
 * @see slack#linkMessage
 * @see slack#testMessage
 * @see slack#uatMessage
 */
def buildMessage() {
	def jobName = jobName()
	def slackHeader = slackHeader()
	def slackArtifacts = getArtifacts()
	slackSend color: 'good', channel: slackChannel, message: slackHeader + slackArtifacts
	def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
	slackSend color: 'good', channel: slackChannel, message: commitLogHeader + getCommitLog()
}


/**
 * Send a 'Website Deployed!' Slack message including
 * <ul>
 *     <li>{@link slack#slackHeader Standard header}</li>
 *     <li>{@link publishLink#call Link to the deployment}</li>
 *     <li>{@link slack#jobName Job name}</li>
 *     <li>Build number</li>
 *     <li>Link to VCS changelog</li>
 *     <li>{@link slack#getCommitLog List of commit messages}</li>
 * </ul>
 *
 * @param inURL http or https url for the deployed website
 * @return a newline-separated string, as described above
 * @see slack#buildMessage
 * @see slack#testMessage
 * @see slack#uatMessage
 */
def linkMessage(String inURL) {
    def jobName = jobName()
    def slackHeader = slackHeader()
    def slackArtifacts = "${inURL}\n"
    slackSend color: 'good', channel: slackChannel, message: slackHeader + slackArtifacts
    def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
    slackSend color: 'good', channel: slackChannel, message: commitLogHeader + getCommitLog()
}

/**
 * Send a 'Test Suite Complete!' Slack message including
 * <ul>
 *     <li>{@link slack#slackHeader Standard header}</li>
 *     <li>{@link slack#getTestSummary Key stats on the test 'health'}</li>
 *     <li>{@link slack#getCoverageSummary Code's test coverage, as a percentage}</li>
 *     <li>{@link slack#getFailedTests List of tests that failed} (if any)</li>
 * </ul>
 * <p>
 *     NB: This method will send out a warning if it finds a total of 0 tests. Projects
 *     truly without associated tests should not be using {@link testStage}.
 * </p>
 *
 * @return a newline-separated string, as described above
 * @see slack#buildMessage
 * @see slack#linkMessage
 * @see slack#uatMessage
 */
def testMessage() {
	def slackHeader = slackHeader() + "\n*Stage*: ${env.STAGE_NAME}\n"
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

/**
 * Internal method to check whether there were failing tests.
 * <p>
 *     Similar (in principle, at least) to {@link postStage#call}.
 * </p>
 *
 * @return false if there were test failures, true otherwise
 */
def isProjectSuccessful() {
    def testResultAction = currentBuild.rawBuild.getAction(TestResultAction.class)
    projectSuccessful = true
    if (testResultAction != null) {
        if (testResultAction.getFailCount() > 0) {
            projectSuccessful = false  
        }
    }
    return projectSuccessful;
}

/**
 * Send out one of two hard-coded security alerts. One if there were failures, the other
 * if there weren't.
 *
 * @return nothing
 * @see slack#isProjectSuccessful
 * @see slack#sendSlackError
 */
def sendNestStatus() {
    if (isProjectSuccessful()) {
        slackSend color: 'good', channel: slackChannel, message: "Nest Security Status Updated" 
    } else {
        slackSend color: 'danger', channel: slackChannel, message: "@here Nest failed to update security status" 
    }

}

/**
 * Send an 'Automation Test Suite Complete!' Slack message including
 * <ul>
 *     <li>{@link slack#slackHeader Standard header}</li>
 *     <li>{@link slack#getTestSummary Key stats on the test 'health'}</li>
 *     <li>{@link uatStage#reportExtents A precise visualization of automation test results}</li>
 *     <li>{@link slack#getFailedTests List of tests that failed} (if any)</li>
 * </ul>
 * <p>
 *     NB: This method will send out a warning if it finds a total of 0 tests. Projects
 *     truly without associated tests should not be using {@link uatStage}.
 * </p>
 *
 * @return a newline-separated string, as described above
 * @see slack#buildMessage
 * @see slack#linkMessage
 * @see slack#testMessage
 * @see uatStage#call
 */
def uatMessage() {
    def slackHeader = slackHeader() + "\n*Stage*: ${env.STAGE_NAME}\n"
    def failedTest = getFailedTests()
    def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
    def reportMessage = "_*Report*_\n" + env.JOB_URL + "Extent-Report/" + "\n"
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

/**
 * Send out a copy of the {@link slack#slackHeader standard header} for the current build.
 * <p>
 *     Use this to quickly test your slack channel integration. To run this as a separate
 *     Stage within your pipeline, use the wrapper {@link slackEcho#call} instead.
 * </p>
 *
 * @return nothing
 * @see slack#getSlackChannel()
 */
def echo() {
	def slackHeader = slackHeader()
	slackSend color: 'good', channel: slackChannel, message: slackHeader
}
