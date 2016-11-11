package repacker.model;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Stream;

import com.badlogic.gdx.math.Matrix4;
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

	private static String divide(String s, int n) {
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
	private static final String[] FIND_CATS = {};

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = { "Brach_hi.tmd"};//"Cow.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0
						|| findS.map(s -> f.getName().contains(s)).filter(s -> s).findAny().isPresent())) {
					System.out.println("Read " + f);
					try {
						ByteBuffer data = Utils.read(f);
						TMD_File file = new TMD_File(data);
						if (FIND_CATS.length > 0 && !Arrays.stream(FIND_CATS)
								.filter(s -> file.category.equalsIgnoreCase(s)).findAny().isPresent())
							continue;

						TMD_Animation a = file.scene.animations.animations[0];
						for (TMD_Channel c : a.channels) {
							TMD_Node n = c.nodeRef;
							if (n == null)
								continue;
							Vector3 tmp3 = new Vector3();
							Quaternion tmpQ = new Quaternion();
							c.value(0, tmp3, tmpQ, true);
							Matrix4 tmp = new Matrix4().set(tmp3, tmpQ);
							System.out.println(n.node_name);
							System.out.println(n.localPosition);
							System.out.println(tmp);
						}
						System.out.println(a.length + "\t" + a.name);
						
						System.out.println(file.scene.sceneGraph(aa -> ""+aa.worldPosition.getRotation(new Quaternion())));

						ModelBuilder.write(f.getName().substring(0, f.getName().length() - 4), file);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
