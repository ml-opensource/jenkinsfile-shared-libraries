import java.util.function.Function

/**
 * This method collates the findings of a set of code quality checks.
 * <p>
 *     If this project has defined <code>env.QUALITY_SERVICES</code, then
 *     we delegate to {@link reportQuality#call}. Otherwise we simply scan
 *     for the following:
 * </p>
 * <ul>
 *     <li>Checkstyle</li>
 *     <li><a href="https://pmd.github.io/">PMD</a></li>
 *     <li>DRY</li>
 *     <li>OpenTasks</li>
 *     <li><a href="https://dwheeler.com/sloccount/">SLOCCount</a></li>
 * </ul>
 * All exceptions that these might trigger are fully ignored.
 * <p>
 *     Android projects may wish to also take advantage of our support
 *     for {@link androidBuildScriptInject#call late-binding gradle scripts}.
 * </p>
 *
 * @param translateToToolset first param to {@link reportQuality#call}; it has
 * the same defaults and semantics as the corresponding param to that function
 * @return nothing
 */
def call(Function<String, List> translateToToolset = prebuiltQualityToolset.&basic) {
	try {
		// See if this project opted into a custom set of quality checks
		if (env.QUALITY_SERVICES instanceof String && env.QUALITY_SERVICES.length() > 0) {
			reportQuality(translateToToolset)
		} else {
			checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*-lint.xml, **/*checkstyle.xml', unHealthy: ''
			pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*pmd.xml', unHealthy: ''
			dry canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '**/*cpd.xml, **/cpdCheck.xml', unHealthy: ''
			openTasks canComputeNew: false, defaultEncoding: '', excludePattern: '**/Libraries/**, **/Pods/**, **/*.framework/**, **/Xcode.app/**, **/build/**', healthy: '', high: 'FIXME,shit,fuck,suck', ignoreCase: true, low: 'deprecated', normal: 'TODO', pattern: '**/*.swift, **/*.java, **/*.kt, **/*.m, **/*.h, **/*.c', unHealthy: ''
		}
		sloccountPublish encoding: '', pattern: '**/*cloc.xml'
	} catch (Exception ignored) {
		// Let us silence all errors
	}
}
