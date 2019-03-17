/**
 * Zip everything in the '.app' directory into a file called 'app.zip'.
 * <p>
 *     If there are multiple '.app' directories, all of them will be
 *     included in the zip file. Any errors are suppressed and ignored.
 * </p>
 *
 * @param appPath where the '.app' directory is located
 * @return nothing
 */
def call(String appPath = "build/Build/Products/Debug-iphonesimulator/") {
	try {
		zip archive: true, dir: "${appPath}", glob: '*.app/**/*', zipFile: "app.zip"
	} catch (Throwable t) {
	}
}
