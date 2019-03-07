/**
 * Update Git submodules.
 * <p>
 *     Essentially, nested git repositories. Run 'man gitsubmodules' for
 *     more information.
 * </p>
 *
 * @return nothing
 * @see checkoutStage#call
 */
def call() {
    sh "git submodule init"
    sh "git submodule update"
}