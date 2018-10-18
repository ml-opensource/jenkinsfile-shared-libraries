def call() {
    sh "git submodule init"
    sh "git submodule update"
}