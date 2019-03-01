/**
 * Run a <code>stage</code> consisting of
 * <pre>
 *     /bin/bash bundle install
 * </pre>
 * and an optional report to {@link slack#qbash Slack} if something
 * goes wrong.
 * <p>
 *     A bundle, for those not in the know, is a collection of
 *     Ruby gems. More details at <a href="https://bundler.io/">
 *     Bundler.io</a>.
 * </p>
 *
 * @return nothing
 */
def install() {
	stage("Install") {
		slack.qbash 'bundle install'
	}
}

/**
 * Run a <code>stage</code> consisting of
 * <pre>
 *     /bin/bash bundle exec rake ${command}
 * </pre>
 * and an optional report to {@link slack#qbash Slack} if something
 * goes wrong.
 * <p>
 *     A bundle, for those not in the know, is a collection of
 *     Ruby gems. Rake is one such gem. More details at
 *     <a href="https://bundler.io/"> Bundler.io</a> and
 *     <a href="https://ruby.github.io/rake/">the official
 *     Rake website</a>.
 * </p>
 *
 * @param command something to pass through Rake
 * @return nothing
 */
def rake(String command = "") {
	slack.qbash "bundle exec rake ${command}"
}