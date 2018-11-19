def call(String appPath = "build/Build/Products/Debug-iphonesimulator/") {
	try {
		zip archive: true, dir: "${appPath}", glob: '*.app/**/*', zipFile: "app.zip"
	} catch (Throwable t) {
	}
}
