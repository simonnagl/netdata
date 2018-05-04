package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.AllArgsConstructor;

/**
 * Wrap an object with a bound parameterized method call.
 * 
 * <p>
 * {@code p -> p.getValue().method(v)}
 * </p>
 * 
 * @param <R>
 *            return type
 */
@AllArgsConstructor
public class BoundMBeanMethodSingleArg<T, R> implements MBeanQuery<R> {

	private final MBeanSingleArgMethod<T, R> method;
	private final T boundParameter;

	@Override
	public R getValue() throws JmxMBeanServerQueryException {
		return method.getValue(boundParameter);
	}

}
