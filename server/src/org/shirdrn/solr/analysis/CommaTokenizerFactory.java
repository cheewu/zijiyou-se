package org.shirdrn.solr.analysis;

import java.io.Reader;
import java.util.Map;

import org.apache.solr.analysis.BaseTokenizerFactory;

public class CommaTokenizerFactory extends BaseTokenizerFactory {

	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		assureMatchVersion();
	}

	public CommaTokenizer create(Reader input) {
		return new CommaTokenizer(luceneMatchVersion, input);
	}

}
