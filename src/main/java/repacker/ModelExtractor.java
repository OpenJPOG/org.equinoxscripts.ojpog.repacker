package repacker;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

import repacker.model.TMD_File;
import repacker.model.export.ModelBuilder_DAE;

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
			"Galli_hi.tmd", "Homalo_hi.tmd", "Pachy_hi.tmd", "Para_hi.tmd", "Raptor_hi.tmd", "Steg_hi.tmd",
			"Styrac_hi.tmd", "TRex_hi.tmd", "Tricera_hi.tmd", "Toro_hi.tmd", "Dryo_hi.tmd", "Edmont_hi.tmd",
			"Kentro_hi.tmd", "Ourano_hi.tmd", "Spino_hi.tmd" };
	private static final String[] FOOD = { "Goat.tmd", "Cow.tmd" };
	private static final String[] HUMANS = { "Cleaner.tmd", "Ranger.tmd", "VisFEnv.tmd", "VisFneut.tmd", "VisFVlnt.tmd",
			"VisMEnv.tmd", "VisMNeut.tmd", "VisMVlnt.tmd" };
	private static final String[] ACTORS = Stream.of(DINOS, FOOD, HUMANS).flatMap(a -> Arrays.stream(a))
			.toArray(a -> new String[a]);

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models/backup").listFiles()) {
				String[] find = { "Allo.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0 || findS
						.filter(s -> f.getName().toLowerCase().contains(s.toLowerCase())).findAny().isPresent())) {
					if (f.getName().equals("HuntPlat.tmd") || f.getName().equals("STurret.tmd")) {
						System.err.println("Can't read strange variation: " + f);
						continue;
					}
					try {
						TMD_File file = new TMD_File(f);
						if (FIND_CATS.length > 0 && !Arrays.stream(FIND_CATS)
								.filter(s -> file.header.category.equalsIgnoreCase(s)).findAny().isPresent())
							continue;

						ModelBuilder_DAE.write(f.getName().substring(0, f.getName().length() - 4), file);
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
