package org.firehol.netdata.module.jmx.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JmxQueryResultTransformationConfiguration {

	/** <code>{}</code> */
	public static final Object PLACEHOLDER = Collections.emptyMap();

	private String method;

	private List<Object> args = new ArrayList<>();

}
