def call(String command) {
    slack.qsh "./gradlew ${command}"
}