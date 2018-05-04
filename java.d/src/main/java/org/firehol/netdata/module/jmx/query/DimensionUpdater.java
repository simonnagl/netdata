package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

public interface DimensionUpdater {

	/** Update values in all known dimensions */
	void updateDimensionValues() throws JmxMBeanServerQueryException;

}
