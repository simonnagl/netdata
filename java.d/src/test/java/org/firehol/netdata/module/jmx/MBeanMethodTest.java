package org.firehol.netdata.module.jmx;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.query.MBeanMethod;
import org.junit.Test;

public class MBeanMethodTest {

	public static class MyMBeanMethod extends MBeanMethod<ThreadInfo> {

		public MyMBeanMethod() throws JmxMBeanServerQueryException {
			super("getThreadInfo", ThreadInfo.class);
			ensureInit(ThreadMXBean.class, Long.class);
			System.out.println(method);
		}
	}

	@Test
	public void testEnsureInit() throws Exception {
		new MyMBeanMethod();
	}

	@Test
	public void testDirect() throws Exception {
		new MBeanMethod<Integer>(ThreadMXBean.class.getMethod("getThreadCount"), Integer.class);
	}
}
