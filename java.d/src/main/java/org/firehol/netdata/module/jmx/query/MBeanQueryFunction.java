package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

public interface MBeanQueryFunction<T, R> {

	R getValue(T param) throws JmxMBeanServerQueryException;
}
