package repacker;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import repacker.model.TMD_File;

public class ModelExtractor {
	static {
		System.loadLibrary("64".equals(System.getProperty("sun.arch.data.model")) ? "gdx64" : "gdx");
	}

	public static String divide(String s, int n) {
		StringBuilder out = new StringBuilder(s.length() + (s.length() / n) + 10);
		for (int i = 0; i < s.length(); i += n) {
			out.append(s.substring(i, Math.min(s.length(), i + n)));
			out.append(' ');
		}
		return out.toString();
	}

	public static String hex(byte[] d) {
		StringBuilder sb = new StringBuilder();
		for (byte f : d) {
			String s = Integer.toHexString(f & 0xFF);
			if (s.length() < 2)
				sb.append("0");
			sb.append(s);
			sb.append(" ");
		}
		return sb.toString();
	}

	public static String pad(String s, int n) {
		while (s.length() < n)
			s = "0" + s;
		return s;
	}

	public static String rpad(String s, int n) {
		while (s.length() < n)
			s = s + " ";
		return s;
	}

	// skinned; { "dc", "dhbja", "djb", "dma", "dmbt", "df", "dha" }
	private static final String[] FIND_CATS = {};// "dc", "dhbja", "djb", "dma",
													// "dmbt", "df", "dha" };

	private static final String[] DINOS = { "Acro_hi.tmd", "Dilopho_hi.tmd", "Alberto_hi.tmd", "Allo_hi.tmd",
			"Anky_hi.tmd", "Brach_hi.tmd", "Camara_hi.tmd", "Carcha_hi.tmd", "Cerato_hi.tmd", "Cory_hi.tmd",
			"Galli_hi.tmd", "Goat.tmd", "Cow.tmd", "Homalo_hi.tmd", "Pachy_hi.tmd", "Para_hi.tmd", "Raptor_hi.tmd",
			"Steg_hi.tmd", "Styrac_hi.tmd", "TRex_hi.tmd" };

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = {};// { "Cow.tmd", "Acro_hi.tmd",
									// "Dilopho_hi.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd")
						&& (find.length == 0 || findS.map(s -> f.getName().toLowerCase().contains(s.toLowerCase()))
								.filter(s -> s).findAny().isPresent())) {
					if (f.getName().equals("HuntPlat.tmd") || f.getName().equals("STurret.tmd")) {
						System.err.println("Can't read strange variation: " + f);
						continue;
					}
					try {
						ByteBuffer data = Utils.read(f);
						TMD_File file = new TMD_File(f.getName().substring(0, f.getName().length() - 4), data);
						if (FIND_CATS.length > 0 && !Arrays.stream(FIND_CATS)
								.filter(s -> file.header.category.equalsIgnoreCase(s)).findAny().isPresent())
							continue;
						// if (data.hasRemaining())
						// System.out.println("Read: " + f + ", leftover " +
						// data.remaining());
						// ModelBuilder.write(f.getName().substring(0,
						// f.getName().length() - 4), file);
					} catch (Exception e) {
						System.err.println("Err reading " + f);
						e.printStackTrace();
						Thread.sleep(1000);
					}
					// System.out.println(TMD_Header_Block.unks);
				}
			}
		}
	}
}
