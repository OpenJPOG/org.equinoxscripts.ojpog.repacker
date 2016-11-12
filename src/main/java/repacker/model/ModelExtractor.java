package repacker.model;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
	private static final String[] FIND_CATS = {};// { "dc", "dhbja", "djb",
													// "dma", "dmbt", "df",
													// "dha" };

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = { "Dilopho_hi.tmd", "Camara_hi.tmd", "WelcCntr_hi.tmd"};
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0
						|| findS.map(s -> f.getName().contains(s)).filter(s -> s).findAny().isPresent())) {
					try {
						ByteBuffer data = Utils.read(f);
						TMD_File file = new TMD_File(data);
						if (FIND_CATS.length > 0 && !Arrays.stream(FIND_CATS)
								.filter(s -> file.category.equalsIgnoreCase(s)).findAny().isPresent())
							continue;
						// System.out.println("Read " + f);
						// System.out.println("unkS1=" + file.scene.unkS1 + "
						// unkS3=" + file.scene.unkS3);
						System.out.println(rpad(f.getName(), 32) + pad(file.scene.animations[0].unk1 + "", 4) + " "
								+ pad(file.scene.animations[0].scene_AnimMeta + "", 8) + "\t"
								+ file.scene.nodes.length);
						for (TMD_Animation a : file.scene.animations) {
							// System.out.println(rpad(a.name, 16) +
							// pad(Integer.toHexString(a.unk1), 8) + " "
							// + Integer.toBinaryString(a.scene_AnimMeta));
							if (1 != 0)
								break;
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
								System.out.println(n.node_name);
								System.out.println(tmp3 + "\tANIM: " + Arrays.stream(c.frames)
										.map(ha -> ha.localPos.toString()).reduce((ff, b) -> ff + ", " + b).get());
								System.out.println(tmpQ + "\tANIM: " + Arrays.stream(c.frames)
										.map(ha -> ha.localRot.toString()).reduce((ff, b) -> ff + ", " + b).get());

								// c.value(0, tmp3, tmpQ, true);
								// Matrix4 tmp = new Matrix4().set(tmp3, tmpQ);
								// System.out.println(n.node_name);
								// System.out.println(n.localPosition);
								// System.out.println(tmp);
							}
							System.out.println();
						}

						// System.out.println(file.scene.nodes.length + "\t" +
						// Arrays.toString(file.scene.nodes));
						// for (TMD_Mesh m : file.meshes.meshes)
						// System.out.println(m.meshParents.length + "\t" +
						// Arrays.toString(m.meshParentsRef));
						// System.out.println();

						ModelBuilder.write(f.getName().substring(0, f.getName().length() - 4), file);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
