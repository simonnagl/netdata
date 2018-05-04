package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;

public interface MappingDimensionUpdater extends DimensionUpdater {

	public void addDimension(MBeanQueryDimensionMapping queryInfo) throws InitializationException;

}
