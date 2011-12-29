package org.shirdrn.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChineseCharacterConverter {
	static Map<String, String> characters = new HashMap<String, String>();
	static {
		try {
			load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static synchronized void load() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(new File("src/characters.txt")));
		String line = null;
		while((line=reader.readLine())!=null) {
			characters.put(line.substring(0, 1), line.substring(1, 2));
		}
	}
	
	public static String translate(String traditionalCharacter) {
		String simplifiedCharacter = characters.get(traditionalCharacter);
		return simplifiedCharacter==null ? traditionalCharacter : simplifiedCharacter;
	}
	
	public static void main(String[] args) {
		System.out.println(translate("礙"));
	}
}
