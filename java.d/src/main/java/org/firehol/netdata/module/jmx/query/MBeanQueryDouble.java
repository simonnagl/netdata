package org.firehol.netdata.module.jmx.query;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.store.MBeanDoubleStore;
import org.firehol.netdata.module.jmx.store.MBeanValueStore;
import org.firehol.netdata.module.jmx.utils.MBeanServerUtils;

import lombok.AccessLevel;
import lombok.Getter;

public class MBeanQueryDouble extends MBeanDefaultQuery<Double, Object> {

	public MBeanQueryDouble(MBeanServerConnection mBeanServerConnection, ObjectName objectName, String attribute) {
		super(objectName, attribute, mBeanServerConnection, Double.class, Object.class);
		mBeanValueStore = new MBeanDoubleStore();
	}

}
