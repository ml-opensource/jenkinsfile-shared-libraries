def call(String nodeName = "any", Boolean checkoutCode = true, Closure body) {
	node(nodeName) {
		prettyPrintDecorator {
			if (checkoutCode) {
				checkoutStage()
			}
			body()
		}
	}
}