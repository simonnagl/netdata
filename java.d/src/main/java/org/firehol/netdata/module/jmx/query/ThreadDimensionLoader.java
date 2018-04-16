package org.firehol.netdata.module.jmx.query;

import org.firehol.netdata.model.Dimension;

public interface ThreadDimensionLoader {

	/** Get or create {@link Dimension} for Java thread id.
	 * 
	 *  @return dimension for the given thread, or <code>null</code> to skip thread
	 */
	public Dimension getOrCreateDimensionFor(long tid);

}
