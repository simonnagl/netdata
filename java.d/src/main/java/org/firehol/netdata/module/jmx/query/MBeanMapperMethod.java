package org.firehol.netdata.module.jmx.query;

import java.lang.reflect.InvocationTargetException;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrap an parameterized query with a unparameterized method call.
 * 
 * <p>
 * {@code p -> p.getValue(...).method()}
 * </p>
 * 
 * @param <T>
 *            parameter type
 * @param <R>
 *            return type
 */
@Getter
@Setter
public class MBeanMapperMethod<T, E, R> extends MBeanMethod<R> implements MBeanQueryFunction<T, R> {

	private final MBeanQueryFunction<T, E> parent;

	public MBeanMapperMethod(MBeanQueryFunction<T, E> parent, String methodName, Class<R> returnType) {
		super(methodName, returnType);
		this.parent = parent;
	}

	@Override
	public R getValue(T param) throws JmxMBeanServerQueryException {
		try {
			E proxy = parent.getValue(param);
			ensureInit(proxy.getClass());
			return returnType.cast(method.invoke(proxy));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JmxMBeanServerQueryException("Failed to call proxy method " + methodName, e);
		}
	}

}
