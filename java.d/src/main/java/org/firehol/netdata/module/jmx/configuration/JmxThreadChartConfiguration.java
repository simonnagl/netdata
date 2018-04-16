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


}