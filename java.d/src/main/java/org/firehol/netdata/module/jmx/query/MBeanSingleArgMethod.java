package org.firehol.netdata.module.jmx.query;

import java.lang.reflect.InvocationTargetException;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import com.sun.jdi.Method;

import lombok.Getter;
import lombok.Setter;

/**
 * Wrap an object with a parameterized method call.
 * 
 * <p>
 * {@code p -> p.getValue().method(...)}
 * </p>
 * 
 * @param <R>
 *            return type
 */
@Getter
@Setter
public class MBeanSingleArgMethod<T, R> extends MBeanMethod<R> implements MBeanQueryFunction<T, R> {

	private final MBeanQuery<?> parent;

	public MBeanSingleArgMethod(MBeanQuery<?> parent, String methodName, Class<R> returnType, Class<T> parameterType) {
		super(methodName, returnType);
		this.parent = parent;
	}

	public MBeanSingleArgMethod(MBeanQuery<?> parent, Method method, Class<R> returnType) {
		super(method, returnType);
		this.parent = parent;
	}

	@Override
	public R getValue(T arg) throws JmxMBeanServerQueryException {
		try {
			Object proxy = parent.getValue();
			ensureInit(proxy.getClass(), arg == null ? Object.class : arg.getClass());
			return returnType.cast(method.invoke(proxy, arg));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JmxMBeanServerQueryException("Failed to call proxy method " + methodName + " of " + parent, e);
		}
	}

}
