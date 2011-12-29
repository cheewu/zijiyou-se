package org.shirdrn.solr.analysis;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.FloatEncoder;
import org.apache.lucene.analysis.payloads.IdentityEncoder;
import org.apache.lucene.analysis.payloads.IntegerEncoder;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.solr.analysis.BaseTokenFilterFactory;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.SolrException;

public class DelimitedPayloadTokenFilterFactory extends BaseTokenFilterFactory {

	public static final String ENCODER_ATTR = "encoder";
	public static final String DELIMITER_ATTR = "delimiter";

	private PayloadEncoder encoder;
	private char delimiter = '|';
	
	public DelimitedPayloadTokenFilter create(TokenStream input) {
		if(null == encoder){
			encoder=new FloatEncoder();
		}
		return new DelimitedPayloadTokenFilter(input,delimiter,encoder);
	}
	
	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		
		String encoderClass = args.get(ENCODER_ATTR);
		if (encoderClass.equals("float")) {
			encoder = new FloatEncoder();
		} else if (encoderClass.equals("integer")) {
			encoder = new IntegerEncoder();
		} else if (encoderClass.equals("identity")) {
			encoder = new IdentityEncoder();
		} else {
			encoder = new FloatEncoder();
		}

		String delim = args.get(DELIMITER_ATTR);
		if (delim != null) {
			if (delim.length() == 1) {
				delimiter = delim.charAt(0);
			} else {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
						"Delimiter must be one character only");
			}
		}
	}

	public void inform(ResourceLoader loader) {
		String encoderClass = args.get(ENCODER_ATTR);
		if (encoderClass.equals("float")) {
			encoder = new FloatEncoder();
		} else if (encoderClass.equals("integer")) {
			encoder = new IntegerEncoder();
		} else if (encoderClass.equals("identity")) {
			encoder = new IdentityEncoder();
		} else {
			encoder = (PayloadEncoder) loader.newInstance(encoderClass);
		}

		String delim = args.get(DELIMITER_ATTR);
		if (delim != null) {
			if (delim.length() == 1) {
				delimiter = delim.charAt(0);
			} else {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
						"Delimiter must be one character only");
			}
		}
	}
}
