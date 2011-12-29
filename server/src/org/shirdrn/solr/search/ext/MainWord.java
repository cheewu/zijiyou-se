package org.shirdrn.solr.search.ext;

import java.util.Set;

public class MainWord {
	private Set<String> wordSet;
	public MainWord(Set<String> wordSet) {
		super();
		this.wordSet = wordSet;
	}
	@Override
	public boolean equals(Object obj) {
		MainWord other = (MainWord) obj;
		if(wordSet.size()!=other.wordSet.size()) {
			return false;
		}
		return true;
	}
	@Override
	public int hashCode() {
		int hashCode = 0;
		for(String word : wordSet) {
			hashCode += word.hashCode();
		}
		return hashCode;
	}
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if(wordSet!=null) {
			for(String word : wordSet) {
				sb.append(word).append(" ");
			}
		}
		return sb.toString().trim();
	}
}
