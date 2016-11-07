package repacker.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import repacker.Base;

public class ModelExtractor {
	static {
		System.loadLibrary("64".equals(System.getProperty("sun.arch.data.model")) ? "gdx64" : "gdx");
	}

	private static String prettyString(float[] f) {
		StringBuilder out = new StringBuilder(f.length * 5);
		out.append('[');
		for (int i = 0; i < f.length; i++) {
			if (i > 0)
				out.append(", ");
			out.append(String.format("%+.02f", f[i]));
		}
		out.append(']');
		return out.toString();
	}

	private static String divide(String s, int n) {
		StringBuilder out = new StringBuilder(s.length() + (s.length() / n) + 10);
		for (int i = 0; i < s.length(); i += n) {
			out.append(s.substring(i, Math.min(s.length(), i + n)));
			out.append(' ');
		}
		return out.toString();
	}

	private static String hex(byte[] d) {
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

	private static String hex(int[] d) {
		StringBuilder sb = new StringBuilder();
		for (int f : d) {
			String s = Long.toHexString(f & 0xFFFFFFFFL);
			sb.append(pad(s, 8));
			sb.append(" ");
		}
		return sb.toString();
	}

	private static final String[] SKINNED_CATS = { "dc", "dhbja", "djb", "dma", "dmbt", "df", "dha" };

	public static void main(String[] args) throws IOException, InterruptedException {
		Map<String, Set<Object>> flags = new HashMap<String, Set<Object>>();

		byte[] buffer = new byte[1024];
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = { "BallRide_hi.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0
						|| findS.map(s -> f.getName().contains(s)).filter(s -> s).findAny().isPresent())) {
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						FileInputStream fin = new FileInputStream(f);
						while (true) {
							int s = fin.read(buffer);
							if (s > 0)
								bos.write(buffer, 0, s);
							else
								break;
						}
						fin.close();
						bos.close();
						ByteBuffer data = ByteBuffer.wrap(bos.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
						TMD_File file = new TMD_File(data);
						// if (!Arrays.stream(SKINNED_CATS).filter(s ->
						// file.category.equalsIgnoreCase(s)).findAny()
						// .isPresent())
						// continue;
						System.out.println(file.scene
								.sceneGraph(node -> divide(pad(Integer.toBinaryString(node.scene_Unk1), 32), 4)));
						ModelBuilder.write(f.getName().substring(0, f.getName().length() - 4), file);

						// needs moving:
						// Door2_01, Door1_02, MainGate03, WCentr12,
						// Door2, Door1
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
