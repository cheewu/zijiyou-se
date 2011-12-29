package org.shirdrn.helper;

import junit.framework.TestCase;

import org.shirdrn.common.DBConfig;

public class DestinationExtendableHelperTest extends TestCase {

	DestinationExtendableHelper helper;
	
	@Override
	protected void setUp() throws Exception {
		DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "POI");
		helper = new DestinationExtendableHelper(config);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testOutput() {
		helper.output("E:\\words-destinations-extendable.dic");
	}
}
