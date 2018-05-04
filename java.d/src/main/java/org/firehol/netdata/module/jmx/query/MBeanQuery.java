package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

public interface MBeanQuery<E> { // <E, T> extends DimensionUpdater {
	//
	// MBean<T> getMBean();
	//
	// /** If exists */
	// MBeanAttrribute<?, T> getMBeanAttribute();

	E getValue() throws JmxMBeanServerQueryException;
}
