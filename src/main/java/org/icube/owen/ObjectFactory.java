package org.icube.owen;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.icube.owen.helper.DatabaseConnectionHelper;
import org.icube.owen.initiative.Initiative;

public class ObjectFactory {

	static public TheBorg getInstance(String className) {
		Class<?> c;
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		Constructor<?> cons;
		try {
			cons = c.getConstructor(String.class);
			TheBorg object = (TheBorg) cons.newInstance();
			return object;
		} catch (NoSuchMethodException | SecurityException
				| InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	static DatabaseConnectionHelper dch;
	static public DatabaseConnectionHelper getDBHelper(){
		if(dch == null ){
			dch = new DatabaseConnectionHelper();
		} return dch;
	}

	public static void main(String[] args) {

		Initiative newinitiative = (Initiative) ObjectFactory.getInstance("owen.initiative.Initiative");

		Initiative getinit = Initiative.get(2);

	}
}
