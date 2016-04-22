package org.icube.owen;

import java.io.File;

import org.apache.log4j.PropertyConfigurator;

public class TheBorg {

	static {
		PropertyConfigurator.configure("resources" + File.separator + "log4j.properties");
	}
	public OwenError errorDetails;
}
