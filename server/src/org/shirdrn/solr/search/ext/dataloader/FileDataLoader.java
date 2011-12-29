package org.shirdrn.solr.search.ext.dataloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.shirdrn.solr.search.ext.MainWord;
import org.shirdrn.util.Pair;

/**
 * Based on file: load data form a given file.
 * 
 * @author shirdrn
 * @date   2011-11-23
 */
public class FileDataLoader implements DataLoader {

	private Map<MainWord, List<Pair<String, Float>>> dataSet;
	private File extFile;
	
	public FileDataLoader() {
		dataSet = new HashMap<MainWord, List<Pair<String, Float>>>();
	}
	
	public FileDataLoader(File extFile) {
		this.extFile = extFile;
		dataSet = new HashMap<MainWord, List<Pair<String, Float>>>();
	}
	
	@Override
	public void loadData() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(extFile.getAbsolutePath()), Charset.forName("UTF-8")));
		String line = "";
		while((line=reader.readLine())!=null) {
			if(line.isEmpty() || line.trim().startsWith("#")) {
				continue;
			}
			String[] a = line.split("=>");
			String main = a[0].trim();
			if(a.length==2 && !main.isEmpty()) {
				MainWord mainWord = new MainWord(parseFrontPart(main));
				List<Pair<String, Float>> extendedWords = parseRearPart(a[1].trim());
				dataSet.put(mainWord, extendedWords);
			}
		}
	}
	
	private static Set<String> parseFrontPart(String frontPart) throws UnsupportedEncodingException {
		Set<String> mainWords = new HashSet<String>();
		String[] a = frontPart.split("[\\s]+");
		for(String s : a) {
			if(s.isEmpty()) {
				continue;
			} else {
				mainWords.add(s.trim());
			}
		}
		return mainWords;
	}
	
	private static List<Pair<String, Float>> parseRearPart(String rearPart) {
		List<Pair<String, Float>> ext = new ArrayList<Pair<String, Float>>();
		String[] a = rearPart.split(",");
		for(String s : a) {
			s = s.trim();
			if(s.indexOf("^")==-1) {
				ext.add(new Pair<String, Float>(s, 1.0f));
			} else {
				String[] ba = s.split("\\^");
				if(!ba[0].isEmpty()) {
					ext.add(new Pair<String, Float>(ba[0].trim(), toFloat(ba[1])));
				}
			}
		}
		return ext;
	}
	
	private static float toFloat(String boostValue) {
		float boost = 1.0f;
		try {
			boost = Float.parseFloat(boostValue);
		} catch (Exception e) {
			boost = 1.0f;
		}
		return boost;
	}

	@Override
	public Object getLoadedDataSet() {
		return dataSet;
	}

	public File getExtFile() {
		return extFile;
	}

	public void setExtFile(File extFile) {
		this.extFile = extFile;
	}

}
