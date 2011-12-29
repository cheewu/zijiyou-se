package org.shirdrn.util;

public class ClassLoaderUtils {

	public static Object newInstance(String className) {
		Object instance = null;
		try {
			instance = Class.forName(className).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return instance;
	}
}
