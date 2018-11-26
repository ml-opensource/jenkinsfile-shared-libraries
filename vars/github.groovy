def isPR() {
	def PRTitle = "${env.CHANGE_TITLE}"
	return !PRTitle.trim().equals("") && !PRTitle.trim().equals("null")
}
