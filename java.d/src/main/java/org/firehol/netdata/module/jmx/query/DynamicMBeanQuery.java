package org.firehol.netdata.module.jmx.query;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.management.InstanceNotFoundException;

import org.firehol.netdata.model.Dimension;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Technical Object which contains information which values retrieved from an
 * {@link MBeanQuery} and where to store the collected values.
 * 
 * @param <T>
 *            the type of parameter
 */
@Getter
@Setter
public class DynamicMBeanQuery<T> implements DimensionUpdater {

	private MBeanQuery<?> parametersQuery;

	private final Logger log = Logger.getLogger("org.firehol.netdata.module.jmx");

	private final Function<String, Dimension> dimensionLoader;

	private final MBeanQueryFunction<T, String> nameQuery;

	private final MBeanQueryFunction<T, Long> valueQuery;

	private final Map<String, Dimension> dimensions = new LinkedHashMap<>();

	private final Map<String, Long> lastValues = new HashMap<>();

	/**
	 * 
	 * @param parametersQuery
	 * @param nameQuery
	 * @param valueQuery
	 * @param dimensionLoader
	 *            get or create dimension for id and name
	 * @throws InstanceNotFoundException
	 * @throws IOException
	 */
	public DynamicMBeanQuery(@NonNull MBeanQuery<?> parametersQuery, MBeanQueryFunction<T, String> nameQuery,
			MBeanQueryFunction<T, Long> valueQuery, @NonNull Function<String, Dimension> dimensionLoader) {
		this.parametersQuery = parametersQuery;
		this.nameQuery = nameQuery;
		this.valueQuery = valueQuery;
		this.dimensionLoader = dimensionLoader;
	}

	/**
	 * Query set of parameters to use for dimensions (e.g., thread IDs).
	 * 
	 * @return
	 * @throws JmxMBeanServerQueryException
	 */
	public Stream<?> queryParametersAsObjectStream() throws JmxMBeanServerQueryException {
		Object value = parametersQuery.getValue();
		if (value.getClass().isArray()) {
			if (!value.getClass().getComponentType().isPrimitive()) {
				return Arrays.stream((Object[]) value);
			}
			if (value instanceof int[]) {
				return Arrays.stream((int[]) value).boxed();
			}
			if (value instanceof long[]) {
				return Arrays.stream((long[]) value).boxed();
			}
			throw new UnsupportedOperationException(
					"Unhandled primitive array of " + value.getClass().getComponentType());
		} else if (value instanceof Collection<?>) {
			return ((Collection<?>) value).stream();
		} else {
			// treat as scalar
			return Stream.of(value);
		}
	}

	protected long queryValue(T parameter) {
		return Long.MIN_VALUE;
	}

	protected String queryName(T parameter) throws JmxMBeanServerQueryException {
		return nameQuery.getValue(parameter);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void updateDimensionValues() throws JmxMBeanServerQueryException {
		for (T parameter : (Iterable<T>) ((Iterable) queryParametersAsObjectStream()::iterator)) {
			String name = getName(parameter);
			final Dimension dimension;
			try {
				dimension = getDimensionLoader().apply(name);
			} catch (RuntimeException e) {
				throw new JmxMBeanServerQueryException("Failed to get dimension for " + name, e);
			}
			if (dimension == null)
				continue; // skip

			// register dimension so that we can clear its value even if it
			// is no longer running
			dimensions.putIfAbsent(dimension.getId(), dimension);

			// collect value
			Long value = valueQuery.getValue(parameter);
			if (value != null) {
				dimension.setCurrentValue(dimension.hasValue() ? dimension.getCurrentValue() + value : value);
			}
		}
	}

	private String getName(T parameter) throws JmxMBeanServerQueryException {
		return nameQuery == null ? String.valueOf(parameter) : nameQuery.getValue(parameter);
	}

}
