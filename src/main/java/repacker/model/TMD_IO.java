package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_IO {
	public static String read(ByteBuffer b, int l) throws UnsupportedEncodingException {
		byte[] tmp = new byte[l];
		b.get(tmp);
		int len = 0;
		while (len < tmp.length && tmp[len++] != 0)
			;
		if (len < tmp.length || tmp[tmp.length - 1] == 0)
			len--;
		return new String(tmp, 0, len);
	}

	public static void skip(ByteBuffer b, int c) throws UnsupportedEncodingException {
		byte[] tmp = new byte[c];
		b.get(tmp);
	}

	public static void skip(ByteBuffer b, int t, int c) throws UnsupportedEncodingException {
		skip(b, t * c);
	}

	public static void ints(ByteBuffer b, int[] n) {
		for (int i = 0; i < n.length; i++)
			n[i] = b.getInt();
	}

	public static short[] shorts(ByteBuffer b, int n) {
		short[] o = new short[n];
		for (int i = 0; i < n; i++)
			o[i] = b.getShort();
		return o;
	}

	public static byte[] bytes(ByteBuffer b, int n) {
		byte[] o = new byte[n];
		b.get(o);
		return o;
	}

	public static float[] floats(ByteBuffer b, int n) {
		float[] o = new float[n];
		for (int i = 0; i < n; i++)
			o[i] = b.getFloat();
		return o;
	}
}
