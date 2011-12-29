package org.shirdrn.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 文件有关的实用类
 */
public class FileUtil {
	
	/**
	 * 根据资源名称得到资源的输入流
	 * @param c 调用类
	 * @param name 名称
	 * @return 流
	 */
	public static InputStream getResourceAsStream(Class c, String name) {
		InputStream ret = null;

		// 1, 首先在类本身的位置寻找
		ret = c.getResourceAsStream(name);
		if (ret != null)
			return ret;

		// 2, 在当前类的classLoader里面找
		ClassLoader currentClassLoader = c.getClassLoader();
		if (currentClassLoader != null)
			ret = currentClassLoader.getResourceAsStream(name);
		if (ret != null)
			return ret;

		// 3, 在当前线程的classLoader里面找
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (contextClassLoader != null) {
			ret = contextClassLoader.getResourceAsStream(name);
		}
		if (ret != null)
			return ret;

		// 4, 在系统的classLoader里面找
		ret = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
		if (ret != null)
			return ret;

		// 5, 直接在文件系统里面找
		try {
			ret = new FileInputStream(name);
		} catch (Exception e) {
			ret = null;
		}
		if (ret != null)
			return ret;

		return ret;
	}

	/**
	 * 根据资源名称得到资源的输入流
	 * @param name 名称
	 * @return 流
	 */
	public static InputStream getResourceAsStream(String name) {
		InputStream ret = null;

		// 3, 在当前线程的classLoader里面找
		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		if (contextClassLoader != null) {
			ret = contextClassLoader.getResourceAsStream(name);
		}
		if (ret != null)
			return ret;

		// 4, 在系统的classLoader里面找
		ret = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
		if (ret != null)
			return ret;

		// 5, 直接在文件系统里面找
		try {
			ret = new FileInputStream(name);
		} catch (Exception e) {
			ret = null;
		}
		if (ret != null)
			return ret;

		return ret;
	}

	/**
	 * 得到子目录的所有文件
	 * @param dir 目录名
	 * @return 所有子文件
	 */
	public static List<String> getSubDirFiles(String dir) {
		List<String> ret = new ArrayList<String>();

		List<File> needDealFiles = new LinkedList<File>();
		File d = new File(dir);
		needDealFiles.add(d);

		while (!needDealFiles.isEmpty()) {
			File f = needDealFiles.remove(0);
			if (f.isDirectory()) {
				File[] sfs = f.listFiles();
				for (File f0 : sfs)
					needDealFiles.add(f0);
			} else if (f.isFile()) {
				ret.add(f.getAbsolutePath());
			}
		}

		return ret;
	}

	/**
	 * 从流中读取字节数组
	 * @param in 流
	 * @return 字节数组
	 */
	public static byte[] readBinary(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (true) {
			int count = in.read(buffer);
			if (count <= 0)
				break;
			out.write(buffer, 0, count);
		}
		return out.toByteArray();
	}
}
