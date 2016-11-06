package repacker;


import java.io.File;

public class Base {
	public static final File WORKING = new File("C:/Users/westin/Programming/ojpog-workspace/");
	public static final File BASE_OUT = new File(WORKING, "data/output/");
	public static final File[] BASE_IN = new File[] { new File(WORKING, "data/original"),
			new File(WORKING, "data/forgotten/") };

}
