package org.firehol.netdata.module.jmx.query;

import java.util.Objects;

import org.firehol.netdata.module.jmx.exception.JmxMBeanServerQueryException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MBeanAttribute<E> implements MBeanQuery<E> {

	private final MBean<?> mBean;

	private final String attribute;

	private final Class<E> attributeType;

	public MBeanAttribute(MBean<?> mBean, String attribute, Class<E> attributeType) {
		this.mBean = mBean;
		this.attribute = attribute;
		this.attributeType = attributeType;
	}

	public boolean queryDestinationEquals(MBeanAttribute<?> mBeanQuery) {
		return Objects.equals(mBean.getName(), mBeanQuery.getMBean().getName())
				&& Objects.equals(attribute, mBeanQuery.getAttribute());
	}

	@Override
	public E getValue() throws JmxMBeanServerQueryException {
		return attributeType.cast(getMBean().getValue(attribute));
	}

	@Override
	public String toString() {
		return "'" + attribute + "' of " + mBean;
	}
}
