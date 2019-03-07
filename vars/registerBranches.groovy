/**
 * Redefine the set of 'protected' branches.
 * <p>
 *     Protected branches, briefly, are VCS branches that
 *     undergo additional scrutiny. They commonly refer to
 *     runtime environments ('dev', 'staging', 'production')
 *     or code maturity markers ('master', 'release').
 * </p>
 * <p>
 *     Designed for use alongside {@link registerBranches#contains}.
 * </p>
 *
 * @param branches an unordered set of branch names
 * @return nothing
 * @see prettyNode#call
 */
def add(String[] branches) {
	env.PROTECTED_BRANCHES = branches.join(",")
}

/**
 * Check whether a given branch name was previously
 * {@link registerBranches#add added} to the set of 'protected'
 * branches.
 * <p>
 *     This set can be directly manipulated through
 *     <code>env.PROTECTED_BRANCHES</code>, but we discourage
 *     doing so.
 * </p>
 *
 * @param branch the name of some VCS branch
 * @return true if it was added to the set, false otherwise
 */
def contains(String branch) {
	if (env.PROTECTED_BRANCHES) {
		branches = env.PROTECTED_BRANCHES.split(",")
		return branches.contains(branch)
	}
	return false
}