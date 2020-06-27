import com.fuzz.artifactstore.ArtifactStore
import com.fuzz.artifactstore.ArtifactStoreAction
import hudson.plugins.clover.CloverBuildAction
import hudson.plugins.cobertura.CoberturaBuildAction
import hudson.plugins.cobertura.targets.CoverageMetric
import hudson.tasks.junit.CaseResult
import hudson.tasks.junit.TestResultAction
import jenkins.plugins.slack.workflow.SlackResponse


/**
 * A 'SlackResponse' object representing the header of a thread of messages.
 *
 * @see slack#ensureThreadAnchor
 */
static SlackResponse threadAnchor = null


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
 * Get the id of the current build's Slack thread (if defined).
 * <p>
 *     If {@link slack#ensureThreadAnchor()} has not yet been
 *     called in this pipeline, this method will return
 *     {@link slack#getSlackChannel()} instead.
 * </p>
 * <p>
 *     Note: attempts to create threaded messages will not work
 *     unless you have enabled the so-called 'bot user mode' on
 *     the Jenkins host. Refer to
 *     <a href="https://github.com/jenkinsci/slack-plugin#bot-user-mode">
 *         this section</a> of the Slack Plugin GitHub repository
 *     for details.
 * </p>
 *
 * @return either env.SLACK_THREAD_ID (if present) or {@link slack#getSlackChannel()}
 */
def getSlackThread() {
	if (env.SLACK_THREAD_ID) {
		return env.SLACK_THREAD_ID
	} else {
		return slackChannel
	}
}

/**
 * Internal method, intended for use by anything calling <code>slackSend</code>.
 * <p>
 *     If env.SLACK_THREAD_ID and {@link slack#threadAnchor} are defined, this
 *     returns immediately.
 * </p>
 * <p>
 *     Otherwise, this sends a very simple 'anchor' message to the
 *     channel and records the 'threadid' associated with that message
 *     in env.SLACK_THREAD_ID.
 * </p>
 *
 * @see slack#getSlackThread()
 * @see slack#slackHeader()
 */
private void ensureThreadAnchor() {
	if (!env.SLACK_THREAD_ID || threadAnchor == null) {
		def slackHeader = slackHeader()

		// Local variable representing the response from Slack's API.
		def slackResponse = slackSend color: 'good', channel: slackChannel, message: slackHeader
		// We keep around the original response to this, so that we can attach emoji.
		threadAnchor = slackResponse
		env.SLACK_THREAD_ID = slackResponse.threadId
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
				script: "git log --pretty=format:'- %s%b [%an] (<${commitURL}%h|%h>) %n' ${currentCommit} \"^${lastSuccessfulCommit}\"",
				returnStdout: true
			)
			if (commits.equals("")) {
				return "No Changes (re-build?)"
			}
		} catch (Throwable t) {
			return "Couldn't get changes (history got changed?)"
		}

		// If we get here, there is at least one commit in the given range
		int commitCount = sh(
			script: "git rev-list --count \"^${lastSuccessfulCommit}\" ${currentCommit} | tr -d '\n'",
			returnStdout: true
		) as int

		// If your workspace has a git emoji, we try to use it here
		return ":git: commit count: ${commitCount}.\n ${commits}"
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
 *     whether this is a Pull Request, that sort of thing. This doesn't include
 *     a list of artifacts or the test status, as those can sometimes take up a
 *     lot of space.
 * </p>
 * <p>
 *     If the caller does not expect this message to be run on the primary
 *     Jenkins server, it may be worth appending {@link slack#nodeDescription}
 *     to the return value.
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
	return slackHeader
}

/**
 * Internal method to create a short textual summary of the current Jenkins Node.
 * <p>
 *     A build may take place over the course of multiple different nodes. This
 *     will only return information about the current one.
 * </p>
 * <p>
 *     If the caller expects this message to be run on the primary Jenkins
 *     server, it is best to avoid this.
 * </p>
 *
 * @return a short newline-terminated string, ready for posting to Slack
 * @see slack#getTestSummary
 * @see slack#PRMessage
 * @see slack#echo
 */
String nodeDescription() {
	return "Built with _*${env.NODE_NAME}*_\n"
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

		// 1. Start with the last 200 lines.
		def logs = currentBuild.rawBuild.getLog(200).reverse()
		def logsToPrint = []
		def addToLogs = true
		for(String logString : logs) {
			// Collate only logs that we care about.
			if (logString.contains("from /Users")) {
				// In the middle of a Ruby Exception. Skip this line.
			} else if (logString.contains("/lib/rails/") || logString.contains("/.rvm/gems/ruby")) {
				// In the middle of a Ruby Info Message. Skip this line.
			} else if(logString.contains("fastlane finished with errors")) {
				// Terminator for iOS fastlane errors. If we have a message, ignore all following lines.
				if (logsToPrint.size() > 0) {
					addToLogs = false
				}
			} else if(logString.contains("[Pipeline]")) {
				// Terminator for Jenkins Pipeline Info. If we have a message, ignore all following lines.
				if (logsToPrint.size() > 0) {
					addToLogs = false
				}
			} else if (logString.contains("at ") && (logString.contains(".java") || logString.contains(".kt") || logString.contains(".groovy"))) {
				// In the middle of a JVM Exception. Skip this line.
			} else if (logString.contains("FAILURE: Build failed with an exception")) {
				// Terminator for JVM Exception. If we have a message, ignore all following lines.
				if (logsToPrint.size() > 0) {
					addToLogs = false
				}
			} else {
				// Add this line to the array.
				if (addToLogs) {
					logsToPrint.add(logString)
				}
			}
		}
		logsToPrint = logsToPrint.reverse()
		logsString = logsToPrint.subList(Math.max(logsToPrint.size() - 20, 0), logsToPrint.size()).join("\n")

		// 2. Send the logs to Slack.
		ensureThreadAnchor()

		// Attach a warning emoji to the thread anchor
		threadAnchor.addReaction("warning")

		// Local variable representing the response from Slack's API.
		//noinspection GroovyUnusedAssignment
		def slackResponse = null

		echo "About to send header to the existing thread..."
		slackResponse = slackSend color: 'danger', channel: slackThread, message: nodeDescription() + message
		echo "...and now trying to add details to that."
		slackSend color: 'danger', channel: slackThread, message:"```${logsString}```"

		if (!errorMessage.contains("script returned exit code 1")) {
			// Attach a no_entry_sign emoji to the existing message...
			echo "Attaching a response to the header message..."
			slackResponse.addReaction("no_entry_sign")
			// ...and send a stacktrace to the DevOps monitoring channel
			echo "...and making two additional notes elsewhere."
			String fullMessage = slackHeader() + nodeDescription() + "${e}"

			slackResponse = slackSend color: 'danger', channel: "jenkins_notifications", message: fullMessage
			slackSend color: 'danger', channel: slackResponse.threadId, message: e.printStackTrace()
		}
	}
}

def sendMessageWithLogs(String message) {
	def logs = currentBuild.rawBuild.getLog(10).reverse()
	logsString = logs.reverse().subList(1, logs.size()).join("\n")

	ensureThreadAnchor()

	slackSend color: 'warning', channel: slackThread, message:message
	slackSend color: 'warning', channel: slackThread, message:"```${logsString}```"
}

/**
 * Send a list of commit messages to Slack.
 * <p>
 *     Above the list we add a simple header with
 *     <ul>
 *         <li>{@link slack#jobName Job name}</li>
 *         <li>Build number</li>
 *         <li>Link to VCS changelog</li>
 *     </ul>
 *     The commits themselves are assembled by {@link slack#getCommitLog}.
 * </p>
 *
 * @see slack#buildMessage
 * @see slack#linkMessage
 */
void sendCommitLogMessage() {
	def jobName = jobName()

	ensureThreadAnchor()

	def commitLogHeader = "${jobName} - #${env.BUILD_NUMBER} <${env.BUILD_URL}/changes|Changes>:\n"
	slackSend color: 'good', channel: slackThread, message: commitLogHeader + getCommitLog()
}

/**
 * Send up to two 'Build Complete!' Slack messages including
 * <ul>
 *     <li>{@link slack#nodeDescription Standard node description}</li>
 *     <li>{@link slack#getArtifacts List of artifacts on current build}</li>
 *     <li>{@link slack#jobName Job name}</li>
 *     <li>Build number</li>
 *     <li>Link to VCS changelog</li>
 *     <li>{@link slack#getCommitLog List of commit messages}</li>
 * </ul>
 * <p>
 *     We delegate the last four of those to {@link slack#sendCommitLogMessage}.
 * </p>
 *
 * @return nothing
 * @see slack#linkMessage
 * @see slack#testMessage
 * @see slack#uatMessage
 */
def buildMessage() {
	def node = nodeDescription()
	def slackArtifacts = getArtifacts()

	ensureThreadAnchor()

	slackSend color: 'good', channel: slackThread, message: node + slackArtifacts
	sendCommitLogMessage()
}


/**
 * Send up to two 'Website Deployed!' Slack messages including
 * <ul>
 *     <li>{@link slack#nodeDescription Standard node description}</li>
 *     <li>{@link publishLink#call Link to the deployment}</li>
 *     <li>{@link slack#jobName Job name}</li>
 *     <li>Build number</li>
 *     <li>Link to VCS changelog</li>
 *     <li>{@link slack#getCommitLog List of commit messages}</li>
 * </ul>
 * <p>
 *     We delegate the last four of those to {@link slack#sendCommitLogMessage}.
 * </p>
 *
 * @param inURL http or https url for the deployed website
 * @return nothing
 * @see slack#buildMessage
 * @see slack#testMessage
 * @see slack#uatMessage
 */
def linkMessage(String inURL) {
	def header = nodeDescription()
	def slackArtifacts = "${inURL}\n"

	ensureThreadAnchor()

	slackSend color: 'good', channel: slackThread, message: header + slackArtifacts
	sendCommitLogMessage()
}

/**
 * Send a 'Test Suite Complete!' Slack message including
 * <ul>
 *     <li>{@link slack#nodeDescription Standard node description}</li>
 *     <li>{@link slack#getTestSummary Key stats on the test 'health'}</li>
 *     <li>{@link slack#getCoverageSummary Code's test coverage, as a percentage}</li>
 *     <li>{@link slack#getFailedTests List of tests that failed} (if any)</li>
 * </ul>
 * <p>
 *     NB: This method will send out a warning if it finds a total of 0 tests. Projects
 *     truly without associated tests should not be using {@link testStage}.
 * </p>
 *
 * @return nothing
 * @see slack#buildMessage
 * @see slack#linkMessage
 * @see slack#uatMessage
 */
def testMessage() {
	def header = nodeDescription() + "\n*Stage*: ${env.STAGE_NAME}\n"
	def failedTest = getFailedTests()
	def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
	def coverageSummary = "_*Code Coverage*_\n" + getCoverageSummary() + "\n"
	def slackTestSummary = testSummary + coverageSummary

	ensureThreadAnchor()

	if (failedTest == null) {
		if (testSummary.contains("No tests found")) {
			slackSend color: 'warning', channel: slackThread, message: header + slackTestSummary
		} else {
			slackSend color: 'good', channel: slackThread, message: header + slackTestSummary
		}
	} else {
		slackSend color: 'warning', channel: slackThread, message: header + slackTestSummary
		slackSend color: 'warning', channel: slackThread, message: failedTest
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
 * Send an 'Automation Test Suite Complete!' Slack message including
 * <ul>
 *     <li>{@link slack#nodeDescription Standard node description}</li>
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
	def header = nodeDescription() + "\n*Stage*: ${env.STAGE_NAME}\n"
	def failedTest = getFailedTests()
	def testSummary = "_*Test Results*_\n" + getTestSummary() + "\n"
	def reportMessage = "_*Report*_\n" + env.JOB_URL + "Extent-Report/" + "\n"
	if (env.TEST_RAIL_ID) {
		def testRailURL = "https://fuzz.testrail.io/index.php?/runs/overview/${env.TEST_RAIL_ID}"
	}
	def slackTestSummary = testSummary + reportMessage

	ensureThreadAnchor()

	if (failedTest == null) {
		if (testSummary.contains("No tests found")) {
			slackSend color: 'warning', channel: slackThread, message: header + slackTestSummary
		} else {
			slackSend color: 'good', channel: slackThread, message: header + slackTestSummary
		}
	} else {
		slackSend color: 'warning', channel: slackThread, message: header + slackTestSummary
		slackSend color: 'warning', channel: slackThread, message: failedTest
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
	def header = slackHeader()
	slackSend color: 'good', channel: slackChannel, message: header
}
