package org.firehol.netdata.module.jmx.query;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.model.Dimension;
import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Technical Object which contains information which attributes of a 
 * {@link ThreadMXBean} we collect and where to store the collected values.
 */
@Getter
@Setter
public class ThreadMXBeanQuery extends MBeanQuery {

	@Getter(AccessLevel.NONE)
	private List<Dimension> dimensions = new LinkedList<>();

	private ThreadMXBean threadMXBean;

	private ThreadDimensionLoader dimensionLoader;

	private ThreadMXBeanQuery(ObjectName name, String attribute, MBeanServerConnection mBeanServer) {
		super(name, attribute, mBeanServer);
		this.threadMXBean = JMX.newMXBeanProxy(mBeanServer, name, ThreadMXBean.class);
	}

	public static ThreadMXBeanQuery getInstance(String attribute, MBeanServerConnection mBeanServer) throws InitializationException {
		try {
			ObjectName name = ObjectName.getInstance(ManagementFactory.THREAD_MXBEAN_NAME);
			boolean hasThreadMXBeans = mBeanServer.isRegistered(name);
			if (!hasThreadMXBeans) {
				throw new InitializationException("JMX connection has no ThreadMXBean");
			}
			ThreadMXBeanQuery threadMXBeanQuery = new ThreadMXBeanQuery(name, attribute, mBeanServer);
			if ("ThreadCpuTime".equals(attribute)) {
				if (!threadMXBeanQuery.threadMXBean.isThreadCpuTimeSupported()) {
					throw new InitializationException("ThreaadMXBean::isThreadCpuTimeSupported() returned false");
				}
			} else {
				throw new InitializationException("Unhandled thread 'value' field: " + attribute);
			}
			return threadMXBeanQuery;
		} catch (MalformedObjectNameException | IOException e) {
			// log.warning(LoggingUtils.buildMessage("MBeanServer has no ThreadMXBean: " + mBeanServer, e));
			throw new InitializationException("Error initializing ThreadMXBean on: " + mBeanServer, e);
		}
	}

	@Override
	public void addDimension(MBeanQueryDimensionMapping queryInfo) {
		this.dimensions.add(queryInfo.getDimension());
	}

	public void query() throws JmxMBeanServerQueryException {
		if (threadMXBean == null) {
			throw new JmxMBeanServerQueryException("Cannot get data for chart without a ThreadMXBean");
		} else {
			// killed threads will not be updated, so we clear all values before updating
			dimensions.forEach(d -> d.setCurrentValue(null));

			// enumerate live threads
			long[] tids = threadMXBean.getAllThreadIds(); // does not include GC/compiler threads
			if ("ThreadCpuTime".equals(attribute)) {
				for (long tid: tids) {
					final Dimension dimension;
					try {
						dimension = dimensionLoader.getOrCreateDimensionFor(tid);
					} catch (RuntimeException e) {
						throw new JmxMBeanServerQueryException("Failed to get dimension for " + tid, e);
					}
					if (dimension == null) continue; // skip

					// register dimension so that we can clear its value even if it is no longer running
					if (!dimensions.contains(dimension)) dimensions.add(dimension);

					// collect value
					long value = threadMXBean.getThreadCpuTime(tid);
					if (value != -1) {
						// thread is alive and CPU time measurement enabled
						// TODO: add support for aggregation of thread groups
						dimension.setCurrentValue(value);
					}
				}
			} else {
				throw new JmxMBeanServerQueryException("Unhandled thread 'value' field: " + attribute);
			}
		}
	}

}