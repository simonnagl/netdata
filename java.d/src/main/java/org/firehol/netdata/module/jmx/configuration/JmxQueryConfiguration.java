package org.firehol.netdata.module.jmx.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration scheme of a JMX property queried by the
 * {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@AllArgsConstructor
@NoArgsConstructor
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

}
