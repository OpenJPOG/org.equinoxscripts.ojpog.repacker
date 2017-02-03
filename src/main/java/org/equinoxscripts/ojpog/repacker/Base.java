package org.equinoxscripts.ojpog.repacker;


import java.io.File;

public class Base {
	public static final File WORKING = new File("../../");
	public static final File BASE_OUT = new File(WORKING, "data/output/");
	
	public static final File BASE_ORIGINAL = new File(WORKING, "data/original");
	public static final File BASE_FORGOTTEN = new File(WORKING, "data/forgotten");
	public static final File BASE_MESOZOIC = new File(WORKING, "data/mesozoic");
//	public static final File[] BASE_IN = new File[] { ,
//			new File(WORKING, "data/forgotten/") };
	public static final File[] BASE_IN = new File[] { new File(WORKING, "data/original") };

}
