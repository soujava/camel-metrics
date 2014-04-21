package io.initium.camel.component.metrics.jmx.reporter;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelMetricsJmxReporterTestSupport extends CamelTestSupport {

	// logging
	private static final String	SELF		= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER		= LoggerFactory.getLogger(SELF);

	// fields
	MBeanServer					mbeanServer	= ManagementFactory.getPlatformMBeanServer();

	public <T> T getValue(final String objectNameName, final String attributeName, final Class<T> type) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			Object value = this.mbeanServer.getAttribute(objectName, attributeName);
			return type.cast(value);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean verifyAttributeExists(final String objectNameName, final String attributeName) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			this.mbeanServer.getAttribute(objectName, attributeName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean verifyAttributeValueLong(final String objectNameName, final String attributeName, final Long expected) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			Object value = this.mbeanServer.getAttribute(objectName, attributeName);
			Long longValue = (Long) value;
			return longValue.equals(expected);
		} catch (Exception e) {
			LOGGER.error("could not convert to Long", e);
			return false;
		}
	}

	public boolean verifyObjectNameIsRegistered(final String objectNameName) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			return this.mbeanServer.isRegistered(objectName);
		} catch (Exception e) {
			return false;
		}
	}

}
