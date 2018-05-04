package org.firehol.netdata.module.jmx.query;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.LongFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.model.Dimension;
import org.firehol.netdata.model.DimensionAlgorithm;
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
public class ThreadMXBeanQuery implements MappingDimensionUpdater {

	private final Logger log = Logger.getLogger("org.firehol.netdata.module.jmx");

	@Getter(AccessLevel.NONE)
	private List<Dimension> dimensions = new LinkedList<>();

	private ThreadMXBean threadMXBean;

	private ThreadDimensionLoader dimensionLoader;

	private Map<String, LongFunction<Long>> perThreadFunctionsByAttribute = null;

	private Map<String, Map<Long, Long>> perThreadValuesByAttribute = new HashMap<>();

	private Map<Long, String> threadNameCache = new HashMap<>();

	private String attribute;

	public ThreadMXBeanQuery(MBean<ThreadMXBean> threadMXBean, String attribute)
			throws InstanceNotFoundException, IOException {
		this(threadMXBean.newMXBeanProxy(), attribute);
	}

	public ThreadMXBeanQuery(ThreadMXBean threadMXBean, String attribute)
			throws InstanceNotFoundException, IOException {
		this.threadMXBean = threadMXBean;
		this.attribute = attribute;
	}

	private void ensureAllPerThreadFunctionsInitialized() throws InitializationException {
		if (perThreadFunctionsByAttribute == null) {
			perThreadFunctionsByAttribute = new HashMap<>();

			// CPU
			if (!getThreadMXBean().isThreadCpuTimeSupported()) {
				log.warning("ThreaadMXBean does not support thread CPU time monitoring");
			} else {
				if (!threadMXBean.isThreadCpuTimeEnabled()) {
					log.warning("ThreaadMXBean supports CPU time measurememnt, but it is currently disabled");
				}
				perThreadFunctionsByAttribute.put("ThreadCpuTime", tid -> getThreadMXBean().getThreadCpuTime(tid));
				perThreadFunctionsByAttribute.put("ThreadUserTime", tid -> getThreadMXBean().getThreadUserTime(tid));
			}

			// contention
			if (!getThreadMXBean().isThreadContentionMonitoringSupported()) {
				log.warning("ThreaadMXBean does not support thread contention monitoring");
			} else {
				if (!getThreadMXBean().isThreadContentionMonitoringEnabled()) {
					log.warning("ThreaadMXBean supports thread contention monitoring, but it is currently disabled");
				}
				perThreadFunctionsByAttribute.put("ThreadInfo.WaitedTime", tid -> {
					ThreadInfo threadInfo = getThreadMXBean().getThreadInfo(tid);
					if (threadInfo == null)
						return null;
					long value = threadInfo.getWaitedTime();
					return value == -1 ? null : value;
				});
				perThreadFunctionsByAttribute.put("ThreadInfo.WaitedCount", tid -> {
					ThreadInfo threadInfo = getThreadMXBean().getThreadInfo(tid);
					if (threadInfo == null)
						return null;
					long value = threadInfo.getWaitedCount();
					return value == -1 ? null : value;
				});
				perThreadFunctionsByAttribute.put("ThreadInfo.BlockedCount", tid -> {
					ThreadInfo threadInfo = getThreadMXBean().getThreadInfo(tid);
					if (threadInfo == null)
						return null;
					long value = threadInfo.getBlockedCount();
					return value == -1 ? null : value;
				});
			}
		}
	}

	public static ThreadMXBeanQuery getInstance(String attribute, MBeanServerConnection mBeanServer)
			throws InitializationException {
		try {
			ObjectName name = ObjectName.getInstance(ManagementFactory.THREAD_MXBEAN_NAME);
			boolean hasThreadMXBeans = mBeanServer.isRegistered(name);
			if (!hasThreadMXBeans) {
				throw new InitializationException("JMX connection has no ThreadMXBean");
			}
			ThreadMXBeanQuery threadMXBeanQuery = new ThreadMXBeanQuery(
					new MBean<>(name, mBeanServer, ThreadMXBean.class), attribute);
			threadMXBeanQuery.ensureAllPerThreadFunctionsInitialized();
			if (!threadMXBeanQuery.perThreadFunctionsByAttribute.containsKey(attribute)) {
				throw new InitializationException("Unhandled thread 'value' field: " + attribute + " (available: "
						+ threadMXBeanQuery.perThreadFunctionsByAttribute.keySet() + ")");
			}
			return threadMXBeanQuery;
		} catch (JMException | IOException e) {
			// log.warning(LoggingUtils.buildMessage("MBeanServer has no
			// ThreadMXBean: " + mBeanServer, e));
			throw new InitializationException("Error initializing ThreadMXBean on: " + mBeanServer, e);
		}
	}

	private Long queryPerThreadValue(long tid) throws JmxMBeanServerQueryException {
		LongFunction<Long> func = perThreadFunctionsByAttribute.get(attribute);
		if (func == null)
			throw new JmxMBeanServerQueryException("Attribute is not supported or is not available: " + attribute);
		return func.apply(tid);
	}

	@Override
	public void addDimension(MBeanQueryDimensionMapping queryInfo) {
		this.dimensions.add(queryInfo.getDimension());
	}

	@Override
	public void updateDimensionValues() throws JmxMBeanServerQueryException {
		if (getThreadMXBean() == null) {
			throw new JmxMBeanServerQueryException("Cannot get data for chart without a ThreadMXBean");
		} else {
			// killed threads will not be updated, so we clear all values before
			// updating
			dimensions.stream().filter(d -> d.getAlgorithm() == DimensionAlgorithm.ABSOLUTE).forEach(
					d -> d.setCurrentValue(null));

			// enumerate live threads
			long[] tids = getThreadMXBean().getAllThreadIds(); // does not
															   // include
															   // GC/compiler
															   // threads
			// evict dead threads from caches
			evictCaches(tids);
			for (long tid : tids) {
				final Dimension dimension;
				try {
					dimension = getDimensionLoader().getOrCreateDimensionFor(tid, getThreadName(tid));
				} catch (RuntimeException e) {
					throw new JmxMBeanServerQueryException("Failed to get dimension for " + tid, e);
				}
				if (dimension == null)
					continue; // skip

				// register dimension so that we can clear its value even if it
				// is no longer running
				if (!dimensions.contains(dimension))
					dimensions.add(dimension);

				// collect value
				Long value = queryPerThreadValue(tid);
				if (value != null) {
					// calculate value updates
					final long increment;
					if (dimension.getAlgorithm() == DimensionAlgorithm.ABSOLUTE) {
						// put or update (aggregate)
						increment = value;
					} else if (dimension.getAlgorithm() == DimensionAlgorithm.INCREMENTAL) {
						// put or update with difference (aggregate)
						Long oldValue = perThreadValuesByAttribute.computeIfAbsent(attribute, x -> new HashMap<>())
								.put(tid, value);
						increment = oldValue == null ? value : value - oldValue;
					} else {
						throw new JmxMBeanServerQueryException("Unhandled algorithm: " + dimension.getAlgorithm());
					}
					// apply value update
					dimension.setCurrentValue(dimension.hasValue() ? dimension.getCurrentValue() + increment : value);
				}
			}

			// TODO: should we remove no longer updated dimensions?
		}
	}

	private void evictCaches(long[] aliveTids) {
		Arrays.sort(aliveTids);
		threadNameCache.entrySet().removeIf(e -> Arrays.binarySearch(aliveTids, e.getKey()) < 0);
		for (Map<Long, ?> valueCache : perThreadValuesByAttribute.values())
			valueCache.entrySet().removeIf(e -> Arrays.binarySearch(aliveTids, e.getKey()) < 0);
	}

	private String getThreadName(long tid) {
		return threadNameCache.computeIfAbsent(tid, tid_ -> {
			try {
				ThreadInfo threadInfo = getThreadMXBean().getThreadInfo(tid);
				if (threadInfo == null)
					return null; // thread not alive
				return threadInfo.getThreadName();
			} catch (Exception e) {
				log.log(Level.WARNING, "Could not get thread name", e);
				return "#" + tid;
			}
		});
	}
}
