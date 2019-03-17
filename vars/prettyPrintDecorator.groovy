/**
 * Wrap <code>body()</code> for a friendlier terminal experience.
 * <p>
 *     This changes
 *     <ul>
 *         <li>the reported terminal type to 'xterm-color'</li>
 *         <li>the reported locale to en_US</li>
 *         <li>the reported character encoding to UTF-8</li>
 *     </ul>
 *     If this is all new to you, I highly recommend starting
 *     your research by looking into the 'terminfo' database and
 *     the 'LC_ALL' environment variable.
 * </p>
 * <p>
 *     NB: These changes only last for the duration of
 *     <code>body()</code>, and are thus not permanent.
 * </p>
 *
 * @param body arbitrary code to run
 * @return nothing
 */
def call(Closure body) {
	ansiColor('xterm') {
		withEnv(['LC_ALL=en_US.UTF-8']) {
			body()
		}
	}
}