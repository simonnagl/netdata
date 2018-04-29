package org.firehol.netdata.module.jmx.configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration scheme of a dimension names in dynamically created dimensions
 * created by the {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@Getter
@Setter
public class JmxNameQueryConfiguration extends JmxQueryConfiguration {

	private String compositeDataKey;

	/**
	 * include only objects whose name matches this pattern (before rewrite, if
	 * any)
	 * 
	 * @see #excludePattern
	 */
	private String includePattern;

	/**
	 * exclude any objects whose name matches this pattern (before rewrite, if
	 * any)
	 * 
	 * @see #includePattern
	 */
	private String excludePattern;

	/**
	 * pattern to use for rewriting object names (e.g. to merge different
	 * objects into a single group)
	 * 
	 * @see #rewriteNameReplacement
	 */
	private String rewritePattern;

	/**
	 * pattern to use for rewriting object names (e.g. to merge different
	 * objects into a single group)
	 * 
	 * @see #rewritePattern
	 */
	private String rewritePatternReplacement;

	/**
	 * set case-sensitivity for all object name patterns
	 * 
	 * @see #includePattern
	 * @see #excludePattern
	 * @see #rewritePattern
	 */
	private boolean patternCaseSensitive = true;

}
