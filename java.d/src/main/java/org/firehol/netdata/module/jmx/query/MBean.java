package org.firehol.netdata.module.jmx.query;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.lang.management.ThreadMXBean;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.utils.MBeanServerUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MBean<T> implements MBeanQuery<T>, MBeanQueryFunction<String, Object> {

	protected final ObjectName name;
	protected MBeanServerConnection mBeanServer;
	protected final Class<T> objectType;
	private volatile T proxy;

	public MBean(ObjectName name, MBeanServerConnection mBeanServer, Class<T> objectType) {
		this.name = name;
		this.mBeanServer = mBeanServer;
		this.objectType = objectType;
	}

	protected T newMXBeanProxy() throws InstanceNotFoundException, IOException {
		if (!PlatformManagedObject.class.isAssignableFrom(objectType))
			throw new UnsupportedOperationException("Object type is not an MXBean: " + objectType);
		boolean isEmitter = mBeanServer.isInstanceOf(name, NotificationEmitter.class.getName());
		return JMX.newMXBeanProxy(mBeanServer, name, objectType, isEmitter);
	}

	protected T newMBeanProxy() throws InstanceNotFoundException, IOException {
		if (PlatformManagedObject.class.isAssignableFrom(objectType))
			throw new UnsupportedOperationException("Object type is an MXBean: " + objectType);
		boolean isEmitter = mBeanServer.isInstanceOf(name, NotificationEmitter.class.getName());
		return JMX.newMBeanProxy(mBeanServer, name, objectType, isEmitter);
	}

	protected T newBeanProxy() throws InstanceNotFoundException, IOException {
		return (PlatformManagedObject.class.isAssignableFrom(objectType)) ? newMXBeanProxy() : newMBeanProxy();
	}

	@Override
	public T getValue() throws JmxMBeanServerQueryException {
		try {
			return proxy == null ? proxy = newBeanProxy() : proxy;
		} catch (InstanceNotFoundException | IOException e) {
			throw new JmxMBeanServerQueryException("Failed to create proxy for " + name, e);
		}
	}

	@Override
	public Object getValue(String attribute) throws JmxMBeanServerQueryException {
		return MBeanServerUtils.getAttribute(mBeanServer, name, attribute);
	}

	@Override
	public String toString() {
		return "'" + getName() + "' at " + mBeanServer;
	}

	public static MBean<?> forName(ObjectName name, MBeanServerConnection mBeanServer)
			throws ClassNotFoundException, IOException, JMException {
		return new MBean<>(name, mBeanServer, guessInterface(name, mBeanServer));
	}

	private static Class<?> guessInterface(ObjectName name, MBeanServerConnection mBeanServer)
			throws ClassNotFoundException, IOException, JMException {
		Class<?> clazz = Class.forName(mBeanServer.getMBeanInfo(name).getClassName());
		for (Class<?> iface : clazz.getInterfaces()) {
			if (PlatformManagedObject.class.isAssignableFrom(iface) && PlatformManagedObject.class != iface)
				return iface;
		}
		throw new IllegalArgumentException("Failed to guess interface of '" + name + "' from class " + clazz);
	}
}
