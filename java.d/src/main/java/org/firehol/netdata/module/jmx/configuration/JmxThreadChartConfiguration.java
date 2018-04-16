package org.firehol.netdata.module.jmx.configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration scheme of a per-thread chart created by the
 * {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@Getter
@Setter
public class JmxThreadChartConfiguration extends JmxChartConfigurationBase {

	/**
	 * dimension this chart displays for each thread
	 */
	private JmxDimensionConfiguration dimensionTemplate;

	/** 
	 * include only threads whose name matches this pattern (before rewrite, if any)
	 * 
	 * @see #excludeNamePattern
	 */
	private String includeNamePattern;

	/** 
	 * exclude any threads whose name matches this pattern (before rewrite, if any)
	 * 
	 * @see #includeNamePattern
	 */
	private String excludeNamePattern;

	/** 
	 * pattern to use for rewriting thread names (e.g. to merge different threads into a single group)
	 * 
	 * @see #rewriteNameReplacement
	 */
	private String rewriteNamePattern;

	/** 
	 * pattern to use for rewriting thread names (e.g. to merge different threads into a single group)
	 * 
	 * @see #rewriteNamePattern
	 */
	private String rewriteNameReplacement;

	/**
	 * set case-sensitivity for all thread name patterns
	 * 
	 * @see #includeNamePattern
	 * @see #excludeNamePattern
	 * @see #rewriteNamePattern
	 */
	private boolean namePatternCaseInsensitive;

}