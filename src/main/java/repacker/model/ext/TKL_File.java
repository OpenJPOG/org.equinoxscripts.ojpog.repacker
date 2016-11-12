package repacker.model.ext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import repacker.Base;
import repacker.Utils;
import repacker.model.TMD_IO;

/**
 * Stores a database of rotations and positions for use in animations.
 */
public class TKL_File extends TMD_IO {
	public static String rpad(String s, int n) {
		while (s.length() < n)
			s = s + " ";
		return s;
	}

	public final Vector3[] positions;
	public final Quaternion[] rotations;
	public final String category;
	private final byte[] unk1 = new byte[4];
	private final byte[] unk2 = new byte[10];
	private final byte[] unk3 = new byte[20];

	public TKL_File(ByteBuffer data) throws IOException {
		if (!read(data, 4).equals("TPKL"))
			throw new IOException("Bad magic");
		data.get(unk1);
		int dataSize = data.getInt();
		if (data.remaining() != dataSize)
			throw new IOException("Bad file length");

		// zero terminated; (some random data?)
		category = read(data, 6);
		data.get(unk2);

		positions = new Vector3[data.getInt()];
		rotations = new Quaternion[data.getInt()];

		data.get(unk3);
		for (int i = 0; i < positions.length; i++)
			positions[i] = Utils.readV3(data);
		for (int i = 0; i < rotations.length; i++)
			rotations[i] = Utils.readQ(data);
	}

	private static final Map<String, TKL_File> tkl = new HashMap<>();

	public static TKL_File tkl(String name) {
		if (tkl.containsKey(name))
			return tkl.get(name);
		byte[] buffer = new byte[1024];
		for (int i = Base.BASE_IN.length - 1; i >= 0; i--) {
			File f = new File(Base.BASE_IN[i], "Data/Models/" + name);
			if (!f.exists())
				continue;
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
				TKL_File file = new TKL_File(data);
//				System.out.println("Loaded TKL file " + name + " (" + file.positions.length + " positions, "
//						+ file.rotations.length + " rotations)");
				tkl.put(name, file);
				return file;
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		TKL_File flora = tkl("flora.tkl");
		System.out.println(Arrays.toString(flora.positions));
		System.out.println(Arrays.toString(flora.rotations));
	}
}
