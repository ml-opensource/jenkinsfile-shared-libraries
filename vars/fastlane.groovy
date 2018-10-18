def call(String command) {
    slack.qsh "fastlane ${command}"
}