package org.firehol.netdata.module.jmx.configuration;

import org.firehol.netdata.module.jmx.configuration.JmxDimensionConfiguration.JmxDimensionConfigurationBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration scheme of a dimension in a dynamic chart created by the
 * {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@Getter
@Setter
public class JmxDimensionTemplateConfiguration {

	/**
	 * Multiply the collected value before displaying it.
	 */
	private int multiplier = 1;
	/**
	 * Divide the collected value before displaying it.
	 */
	private int divisor = 1;

	/**
	 * If true the value get's collected but not displayed.
	 */
	private boolean hidden = false;

	/**
	 * The getter of the attribute must return a list of objects. Each object is
	 * used as parameter for the value field of the
	 * {@link JmxDynamicChartConfiguration#dimensionTemplate}.
	 */
	private JmxQueryConfiguration queryParameter;

	private JmxQueryConfiguration valueQuery;

	/** Optional. */
	private JmxNameQueryConfiguration nameQuery;

	@JsonIgnore
	public JmxDimensionConfigurationBuilder toDimensionConfigurationBuilder() {
		return JmxDimensionConfiguration.builder().multiplier(multiplier).divisor(divisor).hidden(hidden);
	}

	@JsonIgnore
	public JmxDimensionConfiguration buildDimensionConfiguration(String name) {
		return toDimensionConfigurationBuilder().name(name).build();
	}
}
