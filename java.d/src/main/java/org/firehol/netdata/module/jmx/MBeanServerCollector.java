/*
 * Copyright (C) 2017 Simon Nagl
 *
 * netdata is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.firehol.netdata.module.jmx;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.firehol.netdata.exception.InitializationException;
import org.firehol.netdata.exception.UnreachableCodeException;
import org.firehol.netdata.model.Chart;
import org.firehol.netdata.model.Dimension;
import org.firehol.netdata.module.jmx.configuration.JmxChartBaseConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxChartConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxDimensionConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxDynamicChartConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxNameQueryConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxQueryConfiguration;
import org.firehol.netdata.module.jmx.configuration.JmxServerConfiguration;
import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;
import org.firehol.netdata.module.jmx.entity.MBeanQueryInfo;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.query.MBeanQuery;
import org.firehol.netdata.module.jmx.query.ThreadMXBeanQuery;
import org.firehol.netdata.module.jmx.utils.MBeanServerUtils;
import org.firehol.netdata.plugin.Collector;
import org.firehol.netdata.utils.StringUtils;
import org.firehol.netdata.utils.logging.LoggingUtils;
import org.firehol.netdata.utils.logging.NetdataLevel;

import lombok.Getter;

/**
 * Collects metrics of one MBeanServerConnection.
 * 
 * @since 1.0.0
 * @author Simon Nagl
 *
 */
public class MBeanServerCollector implements Collector, Closeable {

	private final Logger log = Logger.getLogger("org.firehol.netdata.module.jmx");

	private JmxServerConfiguration serverConfiguration;

	@Getter
	private final MBeanServerConnection mBeanServer;

	private JMXConnector jmxConnector;

	private List<MBeanQuery<?, ?>> allMBeanQuery = new LinkedList<>();

	private List<Chart> allChart = new LinkedList<>();

	/** sanitize these characters in dimension IDs */
	private static final Pattern BAD_CHAR = Pattern.compile("[^a-zA-Z0-9_]");

	/**
	 * Creates an MBeanServerCollector.
	 * 
	 * <p>
	 * <b>Warning:</b> Only use this when you do not want to close the
	 * underlying JMXConnetor when closing the generated MBeanServerCollector.
	 * </p>
	 * 
	 * @param configuration
	 *            Configuration to apply to this collector.
	 * @param mBeanServer
	 *            to query
	 */
	public MBeanServerCollector(JmxServerConfiguration configuration, MBeanServerConnection mBeanServer) {
		this.serverConfiguration = configuration;
		this.mBeanServer = mBeanServer;
	}

	/**
	 * Creates an MBeanServerCollector.
	 * 
	 * <p>
	 * Calling {@link close()} on the resulting {@code MBeanServerCollector}
	 * closes {@code jmxConnector} too.
	 * </p>
	 * 
	 * @param configuration
	 * @param mBeanServer
	 * @param jmxConnector
	 */
	public MBeanServerCollector(JmxServerConfiguration configuration, MBeanServerConnection mBeanServer,
			JMXConnector jmxConnector) {
		this(configuration, mBeanServer);
		this.jmxConnector = jmxConnector;
	}

	/**
	 * <p>
	 * Queries MBean {@code java.lang:type=Runtime} for attribute {@code Name}.
	 * </p>
	 * 
	 * <p>
	 * This attribute can be used as a unique identifier of the underlying JMX
	 * agent
	 * </p>
	 * 
	 * @return the name representing the Java virtual machine of the queried
	 *         server..
	 * @throws JmxMBeanServerQueryException
	 *             on errors.
	 */
	public String getRuntimeName() throws JmxMBeanServerQueryException {

		// Final names.
		final String runtimeMBeanName = "java.lang:type=Runtime";
		final String runtimeNameAttributeName = "Name";

		// Build object name.
		ObjectName runtimeObjectName;
		try {
			runtimeObjectName = ObjectName.getInstance("java.lang:type=Runtime");
		} catch (MalformedObjectNameException e) {
			throw new UnreachableCodeException("Can not be reached because argument of getInstance() is static.", e);
		}

		// Query mBeanServer.
		Object attribute = queryAttribute(runtimeObjectName, "Name");
		if (attribute instanceof String) {
			String runtimeName = (String) attribute;
			return runtimeName;
		}

		// Error handling
		throw new JmxMBeanServerQueryException(
				LoggingUtils.buildMessage("Expected attribute '", runtimeNameAttributeName, " 'of MBean '",
						runtimeMBeanName, "' to return a string. Instead it returned a '",
						attribute.getClass().getSimpleName().toString(), "'."));

	}

	public Collection<Chart> initialize() throws InitializationException {

		// Step 1
		// Check commonChart configuration
		for (JmxChartBaseConfiguration chartConfig : serverConfiguration.getCharts()) {
			// TODO: use try ... catch so that a single chart does not kill the
			// rest
			final Chart chart;
			if (chartConfig instanceof JmxChartConfiguration) {
				chart = createChart((JmxChartConfiguration) chartConfig);
			} else if (chartConfig instanceof JmxDynamicChartConfiguration) {
				JmxDynamicChartConfiguration dynamicChartConfig = (JmxDynamicChartConfiguration) chartConfig;
				String from = dynamicChartConfig.getDimensionTemplate().getQueryParameter().getFrom();
				if (ManagementFactory.THREAD_MXBEAN_NAME.equals(from)) {
					chart = createThreadChart(dynamicChartConfig);
				} else {
					throw new InitializationException("Unhandled dynamic 'from' field: " + from);
				}
			} else {
				throw new InitializationException("Unhandled chart configuration type: " + chartConfig.getClass());
			}
			allChart.add(chart);
		}

		return allChart;
	}

	private Chart createChart(JmxChartConfiguration chartConfig) throws InitializationException {
		Chart chart = initializeChart(chartConfig);

		// Check if the mBeanServer has the desired sources.
		for (JmxDimensionConfiguration dimensionConfig : chartConfig.getDimensions()) {

			Dimension dimension = initializeDimension(chartConfig, dimensionConfig);
			chart.getAllDimension().add(dimension);

			// Add to queryInfo
			final MBeanQuery<?, ?> queryInfo;
			try {
				queryInfo = initializeMBeanQueryInfo(dimensionConfig);
			} catch (JmxMBeanServerQueryException e) {
				log.log(NetdataLevel.ERROR,
						LoggingUtils.getMessageSupplier("Could not query dimension" + dimensionConfig.getName()
								+ " of chart " + chart.getType() + "." + chart.getId() + ". Skipping it.", e));
				continue;
			}

			Optional<MBeanQuery<?, ?>> foundQueryInfo = allMBeanQuery.stream()
					.filter(presentQueryInfo -> presentQueryInfo.queryDestinationEquals(queryInfo))
					.findAny();

			MBeanQuery<?, ?> query;
			if (!foundQueryInfo.isPresent()) {
				allMBeanQuery.add(queryInfo);
				query = queryInfo;
			} else {
				query = foundQueryInfo.get();
			}

			MBeanQueryDimensionMapping dimensionMapping = new MBeanQueryDimensionMapping();
			dimensionMapping.setDimension(dimension);
			dimensionMapping.setCompositeDataKey(dimensionConfig.getCompositeDataKey());
			query.addDimension(dimensionMapping);
		}
		return chart;
	}

	@Deprecated
	private Chart createThreadChart(JmxDynamicChartConfiguration chartConfig) throws InitializationException {
		Chart chart = initializeChart(chartConfig);
		try {
			Predicate<String> threadNameFilter = compileObjectNameFilter(
					chartConfig.getDimensionTemplate().getNameQuery());
			Function<String, String> threadNameRewriter = compileThreadNameRewrite(
					chartConfig.getDimensionTemplate().getNameQuery());

			ThreadMXBeanQuery threadMXBeanQuery = ThreadMXBeanQuery
					.getInstance(chartConfig.getDimensionTemplate().getValueQuery().getValue(), mBeanServer);
			threadMXBeanQuery.setDimensionLoader((tid, threadName) -> {
				if (!threadNameFilter.test(threadName))
					return null; // skip excluded

				threadName = threadNameRewriter.apply(threadName);
				String dimensionName = chartConfig.getDimensionTemplate().getNameQuery() == null ? "threadId"
						: chartConfig.getDimensionTemplate().getNameQuery().getCompositeDataKey();
				dimensionName = dimensionName.replace("threadId", String.valueOf(tid));
				dimensionName = dimensionName.replace("threadName", threadName);
				final String dimensionId = sanitizeToDimensionId(dimensionName);

				Dimension dimension = chart.getDimensionById(dimensionId);
				if (dimension == null) {

					log.fine("Adding new dimension " + dimensionName + " to " + chart.getType() + "." + chart.getId());
					JmxDimensionConfiguration threadDimensionConfig = chartConfig.getDimensionTemplate()
							.buildDimensionConfiguration(dimensionName);
					dimension = initializeDimension(chartConfig, threadDimensionConfig);
					dimension.setId(dimensionId);
					chart.getAllDimension().add(dimension);
				}
				return dimension;
			});
			allMBeanQuery.add(threadMXBeanQuery);
		} catch (Exception e) {
			throw new InitializationException("Failed to initialize thread chart " + chartConfig.getId(), e);
		}
		return chart;
	}

	private static Function<String, String> compileThreadNameRewrite(JmxNameQueryConfiguration nameQueryConfig) {
		if (nameQueryConfig == null || StringUtils.isBlank(nameQueryConfig.getRewritePattern()))
			return name -> name;

		int flags = Pattern.DOTALL | Pattern.MULTILINE;
		if (!nameQueryConfig.isPatternCaseSensitive())
			flags |= Pattern.CASE_INSENSITIVE;

		Pattern rewriteNamePattern = Pattern.compile(nameQueryConfig.getRewritePattern(), flags);

		final String replacement = nameQueryConfig.getRewritePattern() == null ? ""
				: nameQueryConfig.getRewritePatternReplacement();
		return name -> rewriteNamePattern.matcher(name).replaceAll(replacement);
	}

	private static Predicate<String> compileObjectNameFilter(JmxNameQueryConfiguration nameQueryConfig) {
		Predicate<String> threadNameTest = name -> true;
		if (nameQueryConfig == null) {
			return threadNameTest;
		}

		int flags = Pattern.DOTALL | Pattern.MULTILINE;
		if (!nameQueryConfig.isPatternCaseSensitive())
			flags |= Pattern.CASE_INSENSITIVE;

		// ... AND include
		if (nameQueryConfig.getIncludePattern() != null) {
			Pattern includeNamePattern = Pattern.compile(nameQueryConfig.getIncludePattern(), flags);
			threadNameTest = threadNameTest.and(name -> includeNamePattern.matcher(name).matches());
		}

		// ... AND NOT(exclude)
		if (nameQueryConfig.getExcludePattern() != null) {
			Pattern excludeNamePattern = Pattern.compile(nameQueryConfig.getExcludePattern(), flags);
			threadNameTest = threadNameTest.and(name -> !excludeNamePattern.matcher(name).matches());
		}

		return threadNameTest;
	}

	protected Chart initializeChart(JmxChartBaseConfiguration config) {
		Chart chart = new Chart();

		chart.setType("jmx_" + serverConfiguration.getName());
		chart.setFamily(config.getFamily());
		chart.setId(config.getId());
		chart.setTitle(config.getTitle());
		chart.setUnits(config.getUnits());
		chart.setContext(serverConfiguration.getName());
		chart.setChartType(config.getChartType());
		if (config.getPriority() != null) {
			chart.setPriority(config.getPriority());
		}

		return chart;
	}

	protected Dimension initializeDimension(JmxChartBaseConfiguration chartConfig,
			JmxDimensionConfiguration dimensionConfig) {
		Dimension dimension = new Dimension();
		dimension.setId(dimensionConfig.getName());
		dimension.setName(dimensionConfig.getName());
		dimension.setAlgorithm(chartConfig.getDimensionAlgorithm());
		dimension.setMultiplier(dimensionConfig.getMultiplier());
		dimension.setDivisor(dimensionConfig.getDivisor());

		return dimension;
	}

	protected MBeanQuery<?, ?> initializeMBeanQueryInfo(JmxQueryConfiguration queryConfig)
			throws JmxMBeanServerQueryException {

		// Query once to get dataType.
		ObjectName name = null;
		try {
			name = ObjectName.getInstance(queryConfig.getFrom());
		} catch (MalformedObjectNameException e) {
			throw new JmxMBeanServerQueryException("'" + queryConfig.getFrom() + "' is no valid JMX ObjectName", e);
		} catch (NullPointerException e) {
			throw new JmxMBeanServerQueryException("'' is no valid JMX OBjectName", e);
		}
		Object value = queryAttribute(name, queryConfig.getValue());

		// Add to queryInfo
		MBeanQueryInfo queryInfo = new MBeanQueryInfo();
		queryInfo.setMBeanName(name);
		queryInfo.setMBeanAttribute(queryConfig.getValue());
		queryInfo.setMBeanAttributeExample(value);
		queryInfo.setMBeanServer(mBeanServer);
		MBeanQuery<?, ?> query = MBeanQueryFactory.build(queryInfo);

		return query;
	}

	protected Object queryAttribute(ObjectName name, String attribute) throws JmxMBeanServerQueryException {
		return MBeanServerUtils.getAttribute(mBeanServer, name, attribute);
	}

	public Collection<Chart> collectValues() {
		// Query all attributes and fill charts.
		Iterator<MBeanQuery<?, ?>> queryInfoIterator = allMBeanQuery.iterator();

		while (queryInfoIterator.hasNext()) {
			MBeanQuery<?, ?> queryInfo = queryInfoIterator.next();

			try {
				queryInfo.query();
			} catch (JmxMBeanServerQueryException e) {
				// Stop collecting this value.
				log.log(Level.WARNING, LoggingUtils.buildMessage(
						"Stop collection value '" + queryInfo.getAttribute() + "' of '" + queryInfo.getName() + "'.",
						e));
				queryInfoIterator.remove();
			}
		}

		// Return Updated Charts.
		return allChart;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if (this.jmxConnector != null) {
			this.jmxConnector.close();
		}
	}

	String sanitizeToDimensionId(String name) {
		return BAD_CHAR.matcher(name).replaceAll("_");
	}

	@Override
	public void cleanup() {
		try {
			close();
		} catch (IOException e) {
			log.warning(LoggingUtils.buildMessage("Could not cleanup MBeanServerCollector.", e));
		}
	}

	@Override
	public String toString() {
		return MBeanServerCollector.class.getSimpleName() + "(runtime:'" + serverConfiguration.getName() + "', charts:"
				+ allChart.size() + ")";
	}
}
