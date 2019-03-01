/**
 * Checkout code from a Git repository. This runs as a separate Stage.
 * <p>
 *     This runs (in order) <pre>
 *     checkout scm
 *
 *     git lfs install
 *     git lfs pull
 *
 *     body()
 *     </pre>. The <code>lfs</code> in those two Git
 *     commands refers to an optional package for
 *     <a href="https://git-lfs.github.com/">Large File Support</a>.
 * </p>
 * <p>
 *    In the ideal case, this will have successfully
 *    <ol>
 *       <li>Established a copy of the current repo</li>
 *       <li>Enabled LFS support in said repo</li>
 *       <li>Updated all references to Large Files</li>
 *    </ol>.
 *    If there are no large files in the repo, or if the
 *    repo is not tracked under Git, the LFS commands will
 *    do nothing.
 * </p>
 *
 * @param body code to run within this stage, after the checkout
 * @return nothing
 */
def call(Closure body) {
	stage("Checkout") {
		checkout scm
		try {
			sh 'git lfs install'
			sh 'git lfs pull'
		} catch(Throwable t) {
		}
		if (body != null) {
			body()
		}
	}
}
