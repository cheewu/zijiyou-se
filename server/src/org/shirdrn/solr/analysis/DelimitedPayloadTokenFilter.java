package org.shirdrn.solr.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.index.Payload;

public class DelimitedPayloadTokenFilter extends TokenFilter {

	protected char delimiter = '|';
	protected CharTermAttribute termAtt;//CharTermAttribute
	protected PayloadAttribute payAtt;
	protected PayloadEncoder encoder;
	
	protected DelimitedPayloadTokenFilter(TokenStream input,char delimiter,PayloadEncoder encoder) {
		super(input);
		termAtt = addAttribute(CharTermAttribute.class);
		payAtt = addAttribute(PayloadAttribute.class);
		this.delimiter = delimiter;
		this.encoder = encoder;
	}

	@Override
	public boolean incrementToken() throws IOException {
		boolean result = false;
		  if (input.incrementToken()) {
			  final char[] buffer = termAtt.buffer();
			  final int length = termAtt.length();
			  //look for the delimiter
			  boolean seen = false;
			  for (int i = 0; i < length; i++) {
				  if (buffer[i] == delimiter) {
					  termAtt.copyBuffer(buffer, 0, i);
					  Payload pl=encoder.encode(buffer, i + 1, (length - (i + 1)));
					  payAtt.setPayload(pl);
					  seen = true;
					  break;//at this point, we know the whole piece, so we can exit.  If we don't see the delimiter, then the termAtt is the same
				  }
			  }
			  if (seen == false) {
				  //no delimiter
				  payAtt.setPayload(null);
			  }
			  result = true;
		  }
		  return result;
	  }
}
