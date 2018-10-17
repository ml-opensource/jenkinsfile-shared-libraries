def call(String appPath = "build/Build/Products/Debug-iphonesimulator/") {
	zip archive: true, dir: "${appPath}", glob: '*.app/**/*', zipFile: "app.zip"
}