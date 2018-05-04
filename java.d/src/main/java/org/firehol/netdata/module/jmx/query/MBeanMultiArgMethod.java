package org.firehol.netdata.module.jmx.query;

import java.lang.reflect.InvocationTargetException;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.Getter;
import lombok.NonNull;
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
public class MBeanMultiArgMethod<R> extends MBeanMethod<R> implements MBeanQueryFunction<Object[], R> {

	private final MBeanQuery<?> parent;
	private final Class<?>[] parameterTypes;

	public MBeanMultiArgMethod(@NonNull MBeanQuery<?> parent, @NonNull String methodName, @NonNull Class<R> returnType,
			Class<?>... parameterTypes) {
		super(methodName, returnType);
		this.parent = parent;
		this.parameterTypes = parameterTypes;
	}

	@Override
	public R getValue(Object... args) throws JmxMBeanServerQueryException {
		try {
			Object proxy = parent.getValue();
			ensureInit(proxy.getClass(), parameterTypes);
			return returnType.cast(method.invoke(proxy, args));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JmxMBeanServerQueryException("Failed to call proxy method " + methodName, e);
		}
	}

}
