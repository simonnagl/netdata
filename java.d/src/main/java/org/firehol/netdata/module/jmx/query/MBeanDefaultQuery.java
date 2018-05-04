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

package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.module.jmx.entity.MBeanQueryDimensionMapping;
import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;
import org.firehol.netdata.module.jmx.store.MBeanLongStore;
import org.firehol.netdata.module.jmx.store.MBeanValueStore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Technical Object which contains information which attributes of an M(X)Bean
 * we collect and where to store the collected values.
 */
@Getter
@Setter
public class MBeanDefaultQuery<E> implements MappingDimensionUpdater {

	private MBeanQuery<E> parent;

	@Getter(AccessLevel.NONE)
	protected MBeanValueStore mBeanValueStore = new MBeanLongStore();

	public MBeanDefaultQuery(MBeanQuery<E> parent) {
		this.parent = parent;
	}

	@Override
	public void addDimension(MBeanQueryDimensionMapping queryInfo) {
		if (queryInfo.hasCompositeDataKey())
			throw new UnsupportedOperationException("Composite data key not supported.");
		this.mBeanValueStore.addDimension(queryInfo.getDimension());
	}

	@Override
	public void updateDimensionValues() throws JmxMBeanServerQueryException {
		// expect number
		E value = parent.getValue();
		mBeanValueStore.store(value);
	}

	@Override
	public String toString() {
		return "query:" + parent.toString();
	}
}
