def call() {
    sh "git stash"
    sh "git stash clear"
}