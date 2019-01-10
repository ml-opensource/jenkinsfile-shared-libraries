def add(String[] branches) {
	env.PROTECTED_BRANCHES = branches.join(",")
}

def contains(String branch) {
	if (env.PROTECTED_BRANCHES) {
		branches = env.PROTECTED_BRANCHES.split(",")
		return branches.contains(branch)
	}
	return false
}