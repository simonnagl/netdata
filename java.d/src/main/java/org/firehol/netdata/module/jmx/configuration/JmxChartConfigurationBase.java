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

package org.firehol.netdata.module.jmx.configuration;

import org.firehol.netdata.model.ChartType;
import org.firehol.netdata.model.DimensionAlgorithm;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration scheme of charts created by the
 * {@link org.firehol.netdata.module.jmx.JmxModule}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "class", defaultImpl = JmxChartConfiguration.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = JmxChartConfiguration.class, name = "default"),
		@JsonSubTypes.Type(value = JmxDynamicChartConfiguration.class, name = "dynamic") })
@Getter
@Setter
public abstract class JmxChartConfigurationBase {

	/**
	 * uniquely identifies the chart
	 */
	private String id;
	/**
	 * the text above the chart
	 */
	private String title;
	/**
	 * the label of the vertical axis of the chart, all dimensions added to a
	 * chart should have the same units of measurement
	 */
	private String units;
	/**
	 * the sub-menu on the dashboard
	 */
	private String family;
	/**
	 * the relative priority of the charts as rendered on the web page. Lower
	 * numbers make the charts appear before the ones with higher numbers.
	 */
	private Integer priority;
	/**
	 * the chart type used on the web page.
	 */
	private ChartType chartType = ChartType.LINE;
	/**
	 * how to interpret collected values.
	 */
	private DimensionAlgorithm dimensionAlgorithm = DimensionAlgorithm.ABSOLUTE;

}
