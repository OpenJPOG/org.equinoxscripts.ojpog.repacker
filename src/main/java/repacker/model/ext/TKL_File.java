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

	@Override
	public void write(ByteBuffer b) throws IOException {
		b.put(unk1);
		b.putInt(length() - (unk1.length + 4));
		write(b, 6, category);
		b.put(unk2);
		b.putInt(positions.length);
		b.putInt(rotations.length);
	}

	@Override
	public int length() throws IOException {
		int dataSize = 0;
		dataSize += 6;
		dataSize += unk2.length;
		dataSize += 4 * 4;
		dataSize += positions.length * 3 * 4;
		dataSize += rotations.length * 4 * 4;
		return dataSize + unk1.length + 4;
	}
}
