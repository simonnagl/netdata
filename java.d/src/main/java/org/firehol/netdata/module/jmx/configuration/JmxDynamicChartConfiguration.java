package org.firehol.netdata.module.jmx.configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration scheme of a chart with dynamically created dimensions created
 * by the {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@Getter
@Setter
public class JmxDynamicChartConfiguration extends JmxChartConfigurationBase {

	/**
	 * dimension this chart displays for each object
	 */
	private JmxDimensionTemplateConfiguration dimensionTemplate;

}
