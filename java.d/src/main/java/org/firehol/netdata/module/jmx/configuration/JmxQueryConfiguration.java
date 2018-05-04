package org.firehol.netdata.module.jmx.configuration;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration scheme of a JMX property queried by the
 * {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class JmxQueryConfiguration {

	/**
	 * JMX object name.
	 */
	private String from;
	/**
	 * M(X)Bean property name.
	 */
	private String value;

	/**
	 * If {@link #value} is not a scalar, defines how to extract a scalar value
	 * from it.
	 */
	private String compositeDataKey;

	private List<JmxQueryResultTransformationConfiguration> transform = new ArrayList<>();

}
