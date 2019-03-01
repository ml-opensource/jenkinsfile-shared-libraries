/**
 * Check whether this current build reflects a GitHub pull request.
 *
 * @return true if this is probably a Pull Request, false otherwise
 */
def isPR() {
	def PRTitle = "${env.CHANGE_TITLE}"
	return !PRTitle.trim().equals("") && !PRTitle.trim().equals("null")
}
