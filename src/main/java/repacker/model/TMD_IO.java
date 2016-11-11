package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public abstract class TMD_IO {
	public TMD_File file;

	public TMD_IO(TMD_File file) {
		this.file = file;
	}

	public TMD_IO() {
	}

	public void link() {
	}

	public static String readRaw(ByteBuffer b, int l) throws UnsupportedEncodingException {
		byte[] tmp = new byte[l];
		b.get(tmp);
		return new String(tmp);
	}

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

	public static void shorts(ByteBuffer b, short[] n) {
		for (int i = 0; i < n.length; i++)
			n[i] = b.getShort();
	}

	public static float[] floats(ByteBuffer b, float[] n) {
		for (int i = 0; i < n.length; i++)
			n[i] = b.getFloat();
		return n;
	}
}
