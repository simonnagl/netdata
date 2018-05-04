package org.firehol.netdata.module.jmx.query;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.util.Objects;

import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.NotificationEmitter;
import javax.management.ObjectName;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.utils.MBeanServerUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class MBeanQuery<E, T> {

	protected final ObjectName name;

	protected final String attribute;

	protected MBeanServerConnection mBeanServer;

	protected final Class<E> attributeType;

	protected final Class<T> objectType;

	public boolean queryDestinationEquals(MBeanQuery<?, ?> mBeanQuery) {
		return Objects.equals(name, mBeanQuery.getName()) && Objects.equals(attribute, mBeanQuery.getAttribute());
	}

	/** Register dimension for query */
	public abstract void addDimension(MBeanQueryDimensionMapping mappingInfo) throws InitializationException;

	/** Update values in all known dimensions */
	public abstract void query() throws JmxMBeanServerQueryException;

	protected E queryAttribute() throws JmxMBeanServerQueryException {
		return attributeType.cast(MBeanServerUtils.getAttribute(mBeanServer, getName(), getAttribute()));
	}

	protected T newMXBeanProxy() throws InstanceNotFoundException, IOException {
		if (PlatformManagedObject.class.isAssignableFrom(objectType))
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
}
