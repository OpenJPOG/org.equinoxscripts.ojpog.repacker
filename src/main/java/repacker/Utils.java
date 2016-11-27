package repacker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class Utils {
	public static ByteBuffer read(File f) {
		byte[] buffer = new byte[1024];
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
			return ByteBuffer.wrap(bos.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isPermitted(String s) {
		for (int i = 0; i < s.length(); i++)
			if (!Character.isJavaIdentifierPart(s.charAt(i)))
				return false;
		return true;
	}

	public static String toHexString(Object... f) {
		if (f.length == 1)
			return toHexStringA(f[0]);
		return toHexStringA(f);
	}

	public static String toHexStringA(Object f) {
		StringBuilder s = new StringBuilder("[");
		for (int i = 0; i < Array.getLength(f); i++) {
			Object o = Array.get(f, i);
			long v = ((Number) o).longValue();
			int pad = 16;
			if (o instanceof Long)
				pad = 16;
			else if (o instanceof Integer)
				pad = 8;
			else if (o instanceof Short)
				pad = 4;
			else if (o instanceof Byte)
				pad = 2;
			if (i > 0)
				s.append(", ");
			s.append(ModelExtractor.pad(Long.toHexString(v), pad));
		}
		return s.append("]").toString();
	}

	public static Vector3 nearestSegmentPoint(Vector3 s0, Vector3 s1, Vector3 p) {
		Vector3 v = new Vector3(s1).sub(s0);
		Vector3 w = new Vector3(p).sub(s0);
		float c1 = v.dot(w);
		if (c1 <= 0)
			return s0;
		float c2 = v.dot(v);
		if (c2 <= c1)
			return s1;
		float b = c1 / c2;

		w.set(s0).mulAdd(v, b);
		return w;
	}

	public static Vector3 readV3(ByteBuffer b) {
		return new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
	}

	public static void writeV3(ByteBuffer b, Vector3 v) {
		b.putFloat(v.x);
		b.putFloat(v.y);
		b.putFloat(v.z);
	}

	public static Quaternion unique(Quaternion q) {
		// if (q.w < 0)
		// q.mul(-1);
		return q;
	}

	public static Quaternion readQ(ByteBuffer b) {
		return unique(new Quaternion(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat()));
	}

	public static void writeQ(ByteBuffer b, Quaternion q) {
		b.putFloat(q.x);
		b.putFloat(q.y);
		b.putFloat(q.z);
		b.putFloat(q.w);
	}

	public static void write(File file, ByteBuffer output) throws IOException {
		byte[] data = new byte[output.capacity()];
		output.get(data);
		FileOutputStream fio = new FileOutputStream(file);
		fio.write(data);
		fio.close();
	}
}
