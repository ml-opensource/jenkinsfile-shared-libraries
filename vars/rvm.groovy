/**
 * Enable a specific version of the Ruby programming language.
 * <p>
 *     RVM, of course, is the <a href="https://rvm.io/">Ruby Version Manager</a>.
 * </p>
 *
 * @param version desired Semantic Version of Ruby (e.g. 2.3.0). Defaults to latest stable release
 * @return nothing
 */
def use(String version = "ruby --latest") {
	stage("Configure Environment") {
		bash "rvm install ${version}"
		bash "rvm use ${version}"
		bash "gem install bundle"
	}
}