package org.firehol.netdata.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.firehol.netdata.model.Chart;

/**
 * The class {@code StatefulPrinter} communicates with the caller,
 * emitting chart/dimension definitions dinamically as needed.
 * 
 * The format of the communication is defined <a href=
 * "https://github.com/firehol/netdata/wiki/External-Plugins#netdata-plugins">here</a>
 * 
 * @see Printer
 */
public class StatefulPrinter {

	protected final Map<String, Set<String>> knownDimensionIdsByChart = new HashMap<>();

	/**
	 * Ensures the definitions for the chart and its dimensions are initialized.
	 */
	public void ensureChartInitialized(Chart chart) {
		String chartTypeId = chart.getType() + "." + chart.getId();
		Collection<String> dimensionIds = chart.getAllDimension().stream().map(d -> d.getId()).collect(Collectors.toList());

		// are the chart and all its dimensions already initialized?
		Set<String> knownDimensionIds = knownDimensionIdsByChart.get(chartTypeId);
		if (knownDimensionIds == null || !knownDimensionIds.containsAll(dimensionIds)) {
			// chart or some dimensions not previously seen, emit all chart/dimension definitions
			// (some old dimension definitions may be printed again unnecessarily though) 
			Printer.initializeChart(chart);
			knownDimensionIdsByChart.computeIfAbsent(chartTypeId, x -> new HashSet<>()).addAll(dimensionIds);
		}
	}

	/**
	 * Emits values, also emitting chart/dimension definitions as needed.
	 */
	public void printValues(Chart chart) {
		ensureChartInitialized(chart);
		Printer.collect(chart);
	}
}
