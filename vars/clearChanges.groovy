/**
 * This removes all local changes from the current Git repository.
 * <p>
 *     We use the simple tools of
 *     <a href="https://git-scm.com/book/en/v2/Git-Tools-Stashing-and-Cleaning">
 *         the Git Stash
 *     </a> to make this function.
 * </p>
 *
 * @return nothing
 */
def call() {
    sh "git stash"
    sh "git stash clear"
}