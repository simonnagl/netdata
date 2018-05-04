package org.firehol.netdata.module.jmx.query;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import com.fasterxml.jackson.databind.util.ClassUtil;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MBeanMethod<R> {

	protected final String methodName;
	protected final Class<R> returnType;

	@Setter(value = AccessLevel.NONE)
	protected Method method = null;

	public MBeanMethod(String methodName, Class<R> returnType) {
		this.methodName = methodName;
		this.returnType = returnType;
	}

	public MBeanMethod(Method method, Class<R> returnType) {
		this(method.getName(), returnType);
		System.err.println("Return type " + returnType + " ? assignable from " + method.getReturnType());
		if (returnType.isAssignableFrom(method.getReturnType()))
			throw new ClassCastException(
					"Return type " + returnType + " is not assignable from " + method.getReturnType());
		this.method = method;
	}

	protected void ensureInit(Class<?> iface, Class<?>... parameterTypes) throws JmxMBeanServerQueryException {
		if (method == null) {
			try {
				method = iface.getMethod(methodName, parameterTypes);
			} catch (RuntimeException e) {
				throw new JmxMBeanServerQueryException(
						"Failed to get proxy for method " + methodName + Arrays.toString(parameterTypes), e);
			} catch (NoSuchMethodException e) {
				// try to guess method ignoring differences between assignable
				// types (e.g. boxed/native)
				for (Method m : iface.getMethods()) {
					if (!methodEquals(m, methodName, parameterTypes))
						continue;
					method = m;
					return;
				}
				throw new JmxMBeanServerQueryException("Failed to get or guess proxy for method " + methodName
						+ Arrays.toString(parameterTypes) + ", choices: " + Arrays.toString(iface.getMethods()), e);
			}
		}
	}

	static boolean methodEquals(Method m, String methodName, Class<?>[] parameterTypes) {
		if (!methodName.equals(m.getName()))
			return false;
		if (parameterTypes.length != m.getParameterTypes().length)
			return false;
		for (int i = 0; i < parameterTypes.length; i++) {
			final Class<?> declared = m.getParameterTypes()[i];
			final Class<?> expected = parameterTypes[i];
			if (expected.isArray() != declared.isArray())
				return false;
			if (!declared.isAssignableFrom(declared.isPrimitive() ? ClassUtil.primitiveType(expected) : expected))
				return false;
		}
		return true;
	}

}
