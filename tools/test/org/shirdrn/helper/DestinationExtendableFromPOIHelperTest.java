package org.shirdrn.helper;

import junit.framework.TestCase;

import org.shirdrn.common.DBConfig;

public class DestinationExtendableFromPOIHelperTest extends TestCase {

	DestinationExtendableFromPOIHelper helper;
	
	@Override
	protected void setUp() throws Exception {
		DBConfig config = new DBConfig("192.168.0.184", 27017, "tripfm", "POI");
		helper = new DestinationExtendableFromPOIHelper(config);
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testOutput() {
		helper.output("E:\\words-destinations-extendable-sorted.dic");
	}
}
