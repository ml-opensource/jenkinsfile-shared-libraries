def call(String nodeName = "any", Boolean checkoutCode = true, String cloudName = 'fuzz-ec2', Closure body) {
	ec2 cloud: cloudName, template: nodeName
	node(nodeName) {
		prettyPrintDecorator {
			if (checkoutCode) {
				checkoutStage()
				clearChanges()
			}
			body()
		}
	}
}