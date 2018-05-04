package org.firehol.netdata.module.jmx.query;

import java.lang.reflect.InvocationTargetException;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Wrap an object with a no-arguments method call.
 * 
 * <p>
 * {@code p -> p.getValue().method()}
 * </p>
 * 
 * @param <R>
 *            return type
 */
@Getter
@Setter
public class MBeanNoArgMethod<R> extends MBeanMethod<R> implements MBeanQuery<R> {

	private final MBeanQuery<?> parent;
	private final Class<?>[] parameterTypes;

	public MBeanNoArgMethod(@NonNull MBeanQuery<?> parent, @NonNull String methodName, Class<R> returnType,
			Class<?>... parameterTypes) {
		super(methodName, returnType);
		this.parent = parent;
		this.parameterTypes = new Class<?>[0];
	}

	@Override
	public R getValue() throws JmxMBeanServerQueryException {
		try {
			Object proxy = parent.getValue();
			ensureInit(proxy.getClass(), parameterTypes);
			return returnType.cast(method.invoke(proxy));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JmxMBeanServerQueryException("Failed to call proxy method " + methodName, e);
		}
	}

}
