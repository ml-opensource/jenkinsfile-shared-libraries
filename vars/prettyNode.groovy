def call(String nodeName = "any", Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			body()
		}
	}
}