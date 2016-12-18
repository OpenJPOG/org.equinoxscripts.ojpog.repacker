package repacker.model;

import java.io.IOException;
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

	public static void zero(ByteBuffer b, int n) {
		byte[] t = new byte[n];
		b.get(t);
		for (byte c : t)
			if (c != 0)
				System.err.println("Non zero read");
	}

	public static void writeZero(ByteBuffer b, int n) {
		b.put(new byte[n]);
	}

	public static long bitfield(boolean[] val) {
		long v = 0;
		for (int i = 0; i < val.length; i++)
			if (val[i])
				v |= (1 << i);
		return v;
	}

	public static boolean[] bitfield(int value, int size) throws IOException {
		boolean[] bs = new boolean[size];
		int mask = (1 << size) - 1;
		for (int i = 0; i < size; i++)
			bs[i] = bool((value >> i) & 1);
		if ((value & ~mask) != 0)
			throw new IOException("Bad bitfield value: " + value);
		return bs;
	}

	public static boolean bool(int i) {
		if (i != 0 && i != 1)
			new Exception("Reading as bool: " + i).printStackTrace();
		return i != 0;
	}

	public static String readRaw(ByteBuffer b, int l) throws UnsupportedEncodingException {
		byte[] tmp = new byte[l];
		b.get(tmp);
		return new String(tmp);
	}

	public static void write(ByteBuffer b, int l, String s) {
		byte[] tmp = s.getBytes();
		byte[] ct = new byte[l];
		System.arraycopy(tmp, 0, ct, 0, Math.min(ct.length, tmp.length));
		b.put(ct);
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

	public static void writeInts(ByteBuffer b, int[] n) {
		for (int i = 0; i < n.length; i++)
			b.putInt(n[i]);
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

	public static String hex(int i) {
		return "0x" + Integer.toHexString(i);
	}

	public String bin(byte[] d) {
		StringBuilder sb = new StringBuilder(9 * d.length);
		for (byte f : d) {
			String k = Integer.toBinaryString(f & 0xFF);
			for (int j = k.length(); j < 8; j++)
				sb.append('0');
			sb.append(k);
			sb.append(' ');
		}
		return sb.toString();
	}

	public static String hex(byte[] d) {
		StringBuilder sb = new StringBuilder(3 * d.length);
		for (byte f : d) {
			String s = Integer.toHexString(f & 0xFF);
			if (s.length() < 2)
				sb.append("0");
			sb.append(s);
			sb.append(" ");
		}
		return sb.toString();
	}

	public abstract void write(ByteBuffer b) throws IOException;

	public abstract int length() throws IOException;
}
