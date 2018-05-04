package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.store.MBeanDoubleStore;

public class MBeanQueryDouble extends MBeanDefaultQuery<Double> {

	public MBeanQueryDouble(MBeanQuery<Double> parent) {
		super(parent);
		mBeanValueStore = new MBeanDoubleStore();
	}

}
