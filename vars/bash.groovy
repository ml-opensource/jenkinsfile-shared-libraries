/**
 * Run some shell with the host machine's <code>/bin/bash</code>.
 * <p>
 *     It goes without saying that this is remarkably dangerous.
 *     Try <em>NOT</em> to use this or {@link #silent} if at all
 *     possible.
 * </p>
 * <p>
 *     More details about supported operations and environment
 *     variables and so forth can be found
 *     <a href="https://jenkins.io/doc/pipeline/steps/workflow-durable-task-step/#sh-shell-script">
 *         here
 *     </a>.
 * </p>
 *
 * @param command arbitrary commands. Coreutils should be available
 * @return nothing
 */
def call(String command) {
    sh "#!/bin/bash\n source ~/.bash_profile \n  ${command}"
}

/**
 * See {@link bash#call the main docs} - this does practically the
 * same thing.
 * <p>
 *     No matter what, though, this won't throw any
 *     {@link Throwable}s.
 * </p>
 *
 * @param command arbitrary commands. Coreutils should be available
 * @return nothing
 */
def silent(String command) {
	try {
    	sh "#!/bin/bash\n source ~/.bash_profile \n  ${command}"
	} catch(Throwable t) {

	}
}