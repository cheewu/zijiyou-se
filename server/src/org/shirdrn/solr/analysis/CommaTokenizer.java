package org.shirdrn.solr.analysis;

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.Version;

public class CommaTokenizer extends CharTokenizer {

	public CommaTokenizer(Version matchVersion, Reader in) {
		super(matchVersion, in);
	}

	/**
	 * Collects only characters which do not satisfy (c==',').
	 */
	@Override
	protected boolean isTokenChar(int c) {
		return !(c == ',');
	}

}
