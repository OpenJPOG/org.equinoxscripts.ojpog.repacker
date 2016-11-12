package repacker.model;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import repacker.Base;
import repacker.Utils;
import repacker.model.anim.TMD_Animation;
import repacker.model.anim.TMD_Channel;

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

	private static String hex(int[] d) {
		StringBuilder sb = new StringBuilder();
		for (int f : d) {
			String s = Long.toHexString(f & 0xFFFFFFFFL);
			sb.append(pad(s, 8));
			sb.append(" ");
		}
		return sb.toString();
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
				String[] find = {};// { "Alberto_hi.tmd" };// , "Cory_hi.tmd",
									// "Allo_hi.tmd" };//
									// DINOS;
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0
						|| findS.map(s -> f.getName().contains(s)).filter(s -> s).findAny().isPresent())) {
					try {
						ByteBuffer data = Utils.read(f);
						TMD_File file = new TMD_File(f.getName().substring(0,f.getName().length()-4), data);
						if (FIND_CATS.length > 0 && !Arrays.stream(FIND_CATS)
								.filter(s -> file.category.equalsIgnoreCase(s)).findAny().isPresent())
							continue;
//						if (data.hasRemaining())
//							System.out.println(file.scene.unkS1 + "\t" + file.meshes.unk1);
//						System.out.println("Read: " + f + ", leftover " + data.remaining());
//						System.out.println(file.scene.unkS1 + "\t" + ModelExtractor.hex(file.scene.unk2_Zero) + ", "
//								+ ModelExtractor.hex(file.scene.unk3) + ", " + Arrays.toString(file.scene.unk4) + ", "
//								+ Arrays.toString(file.scene.unk5));
						// System.out.println("Max length " +
						// TMD_Animation.maxNameLen);
						if (1 == 1)
							continue;

						for (TMD_Animation a : file.scene.animations) {
							if (!f.getName().equals("WelcCntr_hi.tmd") && !a.name.equalsIgnoreCase("idle_post_lp"))
								continue;
							System.out.println(a.name + "\t" + a.unk1 + "\t" + a.scene_AnimMeta);
							for (TMD_Channel c : a.channels) {
								TMD_Node n = c.nodeRef;
								if (n == null)
									continue;
								Vector3 tmp3 = new Vector3();
								Quaternion tmpQ = new Quaternion();
								n.localPosition.getRotation(tmpQ);
								n.localPosition.getTranslation(tmp3);
								if (n.node_name.startsWith("T_") || n.node_name.startsWith("D_")
										|| n.node_name.startsWith("L_"))
									continue;
								System.out.println(
										n.node_name + "\t" + c.unk1 + "\t" + Integer.toHexString(c.anim_NodeMeta));
								System.out.print(Arrays.stream(c.frames).mapToDouble(ha -> ha.localPos.dst(tmp3)).min()
										.getAsDouble() + "\t");
								System.out.println(Arrays.stream(c.frames)
										.mapToDouble(
												ha -> new Quaternion(tmpQ).conjugate().mulLeft(ha.localRot).getAngle())
										.min().getAsDouble() + " deg");
							}
							System.out.println();
						}

						ModelBuilder.write(f.getName().substring(0, f.getName().length() - 4), file);
					} catch (Exception e) {
						System.err.println("Err reading " + f);
						e.printStackTrace();
						Thread.sleep(1000);
					}
				}
			}
		}
	}
}
