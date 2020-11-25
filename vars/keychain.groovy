/**
 * Execute the slack.qbash with the security command.
 * <p>
 * This will allow builds to be able to run the security command.
 *
 * keycahin.unlock will unlock the keychain
 *
 * </p>
 *
 * @param password the keychain password
 * @param keychainLocation the location of the keychain
 * @return nothing
 */
def unlock(String password, String keychainLocation) {
    slack.qbash "security -v unlock-keychain -p ${password} ${keychainLocation}"
}
