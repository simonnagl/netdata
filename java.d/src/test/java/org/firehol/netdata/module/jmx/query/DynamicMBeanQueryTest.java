package org.firehol.netdata.module.jmx.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.firehol.netdata.model.Dimension;
import org.firehol.netdata.model.DimensionAlgorithm;
import org.junit.Test;

public class DynamicMBeanQueryTest {

	public static class Sleeper extends Thread {

		public Sleeper(String name) {
			super(name);
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					interrupt();
				}
			}
		}
	}

	@Test(timeout = 1000)
	public void testThreadCPU() throws Exception {
		Map<String, Dimension> dimensions = new LinkedHashMap<>();

		// JMX endpoint
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

		// ThreadMXBean
		MBean<ThreadMXBean> threadMXBean = new MBean<>(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME),
				mBeanServer, ThreadMXBean.class);

		// related ThreadMXBean methods/attributes
		MBeanAttribute<long[]> getAllThreadIds = new MBeanAttribute<>(threadMXBean, "AllThreadIds", long[].class);
		MBeanQueryFunction<Long, Long> getThreadCputTime = new MBeanSingleArgMethod<>(threadMXBean, "getThreadCpuTime",
				Long.class, long.class);
		MBeanQueryFunction<Long, ThreadInfo> getThreadInfo = new MBeanSingleArgMethod<>(threadMXBean, "getThreadInfo",
				ThreadInfo.class, long.class);

		// parts for a dynamic collector
		MBeanQuery<?> parametersQuery = getAllThreadIds;
		MBeanQueryFunction<Long, String> nameQuery = new MBeanMapperMethod<Long, ThreadInfo, String>(getThreadInfo,
				"getThreadName", String.class);
		MBeanQueryFunction<Long, Long> valueQuery = getThreadCputTime;

		// the dynamic "chart"
		DimensionUpdater threadCPUChart = new DynamicMBeanQuery<Long>(parametersQuery, nameQuery, valueQuery,
				name -> dimensions.computeIfAbsent((String) name,
						_name -> initializeDimension(_name, DimensionAlgorithm.INCREMENTAL)));

		// test drive the collector:
		// #1: vanilla -> some threads have used some amount of CPU
		// #2: start thread -> new dimension should be added with some value
		// #3: stop thread -> dimension should have no value set

		// pre-flight checks
		assertEquals(0, dimensions.size());
		Thread canary = new Sleeper(DynamicMBeanQueryTest.class.getSimpleName() + "_canary");
		assertFalse(canary.isAlive());

		// flight #1 without the canary
		threadCPUChart.updateDimensionValues();
		assertNotEquals(0, dimensions.size());
		assertFalse(dimensions.containsKey(canary.getName()));
		// dimensions.values().forEach(d -> System.out.println("SET " +
		// d.getName() + " =\t" + d.getCurrentValue()));
		dimensions.values().forEach(d -> {
			if (canary.getName().equals(d.getName())) {
				assertNotNull("dimension " + d.getId() + " has no value set", d.getCurrentValue());
			}
			d.setCurrentValue(null);
		});

		// flight #2 with the canary running
		canary.start();
		assertTrue(canary.isAlive());
		threadCPUChart.updateDimensionValues();
		assertNotEquals(0, dimensions.size());
		assertTrue(dimensions.containsKey(canary.getName()));
		// dimensions.values().forEach(d -> System.out.println("SET " +
		// d.getName() + " =\t" + d.getCurrentValue()));
		dimensions.values().forEach(d -> {
			assertNotNull(d.getCurrentValue());
			d.setCurrentValue(null);
		});

		// flight #3 with the canary dead
		canary.interrupt();
		canary.join();
		assertFalse(canary.isAlive());
		threadCPUChart.updateDimensionValues();
		assertNotEquals(0, dimensions.size());
		assertTrue(dimensions.containsKey(canary.getName()));
		// dimensions.values().forEach(d -> System.out.println("SET " +
		// d.getName() + " =\t" + d.getCurrentValue()));
		dimensions.values().forEach(d -> {
			if (canary.getName().equals(d.getName())) {
				assertNull("dimension " + d.getId() + " has no value set", d.getCurrentValue());
			}
			d.setCurrentValue(null);
		});
	}

	private Dimension initializeDimension(String name, DimensionAlgorithm algorithm) {
		Dimension dimension = new Dimension();
		dimension.setId(name); // may not be a valid ID, but we don't care
		dimension.setName(name);
		dimension.setAlgorithm(algorithm);

		return dimension;
	}

}
