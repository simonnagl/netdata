package org.firehol.netdata.module.jmx;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.firehol.netdata.module.jmx.query.DynamicMBeanQuery;
import org.firehol.netdata.module.jmx.query.MBean;
import org.firehol.netdata.module.jmx.query.MBeanAttribute;
import org.firehol.netdata.module.jmx.query.MBeanMapperMethod;
import org.firehol.netdata.module.jmx.query.MBeanQuery;
import org.firehol.netdata.module.jmx.query.MBeanQueryFunction;
import org.firehol.netdata.module.jmx.query.MBeanSingleArgMethod;
import org.junit.Test;

public class DynamicTest {

	@Test
	public void test() throws Exception {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

		MBean<ThreadMXBean> threadMXBean = new MBean<>(new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME),
				mBeanServer, ThreadMXBean.class);
		MBeanQueryFunction<Long, ThreadInfo> getThreadInfo = new MBeanSingleArgMethod<>(threadMXBean, "getThreadInfo",
				ThreadInfo.class, long.class);
		MBeanAttribute<long[]> getAllThreadIds = new MBeanAttribute<>(threadMXBean, "AllThreadIds", long[].class);

		MBeanQueryFunction<Long, Long> getThreadCputTime = new MBeanSingleArgMethod<>(threadMXBean, "getThreadCpuTime",
				Long.class, long.class);

		MBeanQuery<?> parametersQuery = getAllThreadIds;
		MBeanQueryFunction<Long, String> nameQuery = new MBeanMapperMethod<Long, ThreadInfo, String>(getThreadInfo,
				"getThreadName", String.class);
		MBeanQueryFunction<Long, Long> valueQuery = getThreadCputTime;

		DynamicMBeanQuery dyn = new DynamicMBeanQuery(parametersQuery, nameQuery, valueQuery, id -> null);
		List<?> parameters = (List<?>) dyn.queryParametersAsObjectStream().collect(Collectors.toList());
		for (Object param : parameters) {
			System.err.println(param + " :: " + param.getClass().getSimpleName());
			Object name = dyn.getNameQuery().getValue(param);
			System.err.println(" > " + name + " :: " + name.getClass().getSimpleName());
			Object value = dyn.getValueQuery().getValue(param);
			System.err.println(" = " + value + " :: " + value.getClass().getSimpleName());
		}

		System.err.println(long.class.getName());
	}
}
