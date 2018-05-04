package org.firehol.netdata.module.jmx;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.module.jmx.configuration.JmxQueryConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxQueryResultTransformationConfiguration;
import org.firehol.netdata.module.jmx.entity.MBeanQueryInfo;
import org.firehol.netdata.module.jmx.query.BoundMBeanMethodSingleArg;
import org.firehol.netdata.module.jmx.query.DimensionUpdater;
import org.firehol.netdata.module.jmx.query.MBean;
import org.firehol.netdata.module.jmx.query.MBeanAttribute;
import org.firehol.netdata.module.jmx.query.MBeanDefaultQuery;
import org.firehol.netdata.module.jmx.query.MBeanMapperMethod;
import org.firehol.netdata.module.jmx.query.MBeanNoArgMethod;
import org.firehol.netdata.module.jmx.query.MBeanQuery;
import org.firehol.netdata.module.jmx.query.MBeanQueryCompositeData;
import org.firehol.netdata.module.jmx.query.MBeanQueryDouble;
import org.firehol.netdata.module.jmx.query.MBeanQueryFunction;
import org.firehol.netdata.module.jmx.query.MBeanSingleArgMethod;

public final class MBeanQueryFactory {

	private MBeanQueryFactory() {
	}

	public static DimensionUpdater build(MBeanQueryInfo queryInfo) {
		MBean<?> mBean = new MBean<>(queryInfo.getMBeanName(), queryInfo.getMBeanServer(), Object.class);
		if (Double.class.isAssignableFrom(queryInfo.getMBeanAttributeExample().getClass())) {
			MBeanQuery<Double> query = new MBeanAttribute<>(mBean, queryInfo.getMBeanAttribute(), Double.class);
			return new MBeanQueryDouble(query);
		}

		if (CompositeData.class.isAssignableFrom(queryInfo.getMBeanAttributeExample().getClass())) {
			MBeanQuery<CompositeData> query = new MBeanAttribute<>(mBean, queryInfo.getMBeanAttribute(),
					CompositeData.class);
			return new MBeanQueryCompositeData(query, Number.class);
		}

		MBeanQuery<?> query = new MBeanAttribute<>(mBean, queryInfo.getMBeanAttribute(), Object.class);
		return new MBeanDefaultQuery<>(query);
	}

	public static MBeanQuery<?> buildQuery(MBeanServerConnection mBeanServer, JmxQueryConfiguration queryParameter)
			throws InitializationException {
		try {
			return buildQueryOrFunction(mBeanServer, queryParameter, MBeanQuery.class);
		} catch (RuntimeException | JMException | IOException | ClassNotFoundException e) {
			throw new InitializationException(e);
		}
	}

	public static MBeanQueryFunction<?, ?> buildQueryFunction(MBeanServerConnection mBeanServer,
			JmxQueryConfiguration queryParameter) throws InitializationException {
		try {
			return buildQueryOrFunction(mBeanServer, queryParameter, MBeanQueryFunction.class);
		} catch (RuntimeException | JMException | IOException | ClassNotFoundException e) {
			throw new InitializationException(e);
		}
	}

	private static <E> E buildQueryOrFunction(MBeanServerConnection mBeanServer, JmxQueryConfiguration queryParameter,
			Class<E> expectedType) throws JMException, ClassNotFoundException, IOException {
		final boolean functionNeeded;
		if (expectedType == MBeanQueryFunction.class)
			functionNeeded = true;
		else if (expectedType == MBeanQuery.class)
			functionNeeded = false;
		else
			throw new IllegalArgumentException("Unsupported type: " + expectedType);

		ObjectName name = ObjectName.getInstance(queryParameter.getFrom());
		MBean<?> mBean = MBean.forName(name, mBeanServer);

		// we have either a query or a query function
		MBeanQuery<?> query = queryParameter.getValue() == null ? mBean
				: new MBeanAttribute<>(mBean, queryParameter.getValue(), Object.class);
		MBeanQueryFunction<?, ?> queryFunction = null;

		// backward compatibility: transform with .get($value)
		if (queryParameter.getCompositeDataKey() != null) {
			MBeanSingleArgMethod<String, Object> getMethod = new MBeanSingleArgMethod<String, Object>(query, "get",
					Object.class, String.class);
			query = new BoundMBeanMethodSingleArg<String, Object>(getMethod, queryParameter.getCompositeDataKey());
		}

		// apply transformations
		for (JmxQueryResultTransformationConfiguration transform : toList(queryParameter.getTransform())) {
			if (transform.getArgs() == null || transform.getArgs().size() == 0) {
				// no args
				if (query != null) {
					query = new MBeanNoArgMethod<>(query, transform.getMethod(), Object.class);
				} else {
					queryFunction = new MBeanMapperMethod<>(queryFunction, transform.getMethod(), Object.class);
				}
			} else if (transform.getArgs().size() == 1) {
				final MBeanSingleArgMethod<Object, Object> method = new MBeanSingleArgMethod<Object, Object>(query,
						transform.getMethod(), Object.class, Object.class);
				Object arg = transform.getArgs().get(0);
				if (JmxQueryResultTransformationConfiguration.PLACEHOLDER.equals(arg)) {
					if (!functionNeeded)
						throw new IllegalStateException("cannot use placeholder here: " + queryParameter);
					queryFunction = method;
					query = null;
				} else {
					query = new BoundMBeanMethodSingleArg<Object, Object>(method, arg);
				}
			}
		}
		if (functionNeeded && queryFunction == null)
			throw new IllegalStateException("should use placeholder here: " + queryParameter);

		return expectedType.cast(functionNeeded ? queryFunction : query);
	}

	private static <E> List<E> toList(List<E> list) {
		return list == null ? Collections.emptyList() : list;
	}

}
