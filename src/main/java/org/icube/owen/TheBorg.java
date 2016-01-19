package org.icube.owen;

import org.apache.log4j.PropertyConfigurator;

public class TheBorg {

	static {
		PropertyConfigurator.configure("resources/log4j.properties");

	}
	public OwenError errorDetails;
}
