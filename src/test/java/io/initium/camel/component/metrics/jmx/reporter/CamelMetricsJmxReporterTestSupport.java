package io.initium.camel.component.metrics.jmx.reporter;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.test.junit4.CamelTestSupport;

public class CamelMetricsJmxReporterTestSupport extends CamelTestSupport {
	MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

	public boolean verifyObjectNameIsRegistered(String objectNameName) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			return mbeanServer.isRegistered(objectName);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean verifyAttributeExists(String objectNameName, String attributeName) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			mbeanServer.getAttribute(objectName, attributeName);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean verifyAttributeValueLong(String objectNameName, String attributeName, Long expected) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			Object value = mbeanServer.getAttribute(objectName, attributeName);
			Long doubleValue = (Long) value;
			return doubleValue.equals(expected);
		} catch (Exception e) {
			return false;
		}
	}
	
	public <T> T getValue(String objectNameName, String attributeName, Class<T> type) {
		try {
			ObjectName objectName = new ObjectName(objectNameName);
			Object value = mbeanServer.getAttribute(objectName, attributeName);
			return type.cast(value);
		} catch (Exception e) {
			return null;
		}
	}

}
