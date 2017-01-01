package repacker.texture.dds;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import repacker.Gen_IO;

public class DDS_File extends Gen_IO {
	// Required
	public static final int DDSD_CAPS = 0x1;
	public static final int DDSD_HEIGHT = 0x2;
	public static final int DDSD_WIDTH = 0x4;
	// Required for uncompressed textures
	public static final int DDSD_PITCH = 0x8;
	// Required
	public static final int DDSD_PIXELFORMAT = 0x1000;
	// Required when mipmaps>1
	public static final int DDSD_MIPMAPS = 0x20000;
	// Required for compressed textures
	public static final int DDSD_LINEARSIZE = 0x800000;
	// Required for volume textures
	public static final int DDSD_DEPTH = 0x800000;

	// if have alpha data
	public static final int DDPF_ALPHAPIXELS = 0x1;
	// uncompressed alpha-only
	public static final int DDPF_ALPHA = 0x2;
	// if compressed
	public static final int DDPF_FOURCC = 0x4;
	// uncompressed RGB
	public static final int DDPF_RGB = 0x8;
	// YUV instead of RGB
	public static final int DDPF_YUV = 0x200;
	// uncompressed Luminance == R
	public static final int DDPF_LUMINANCE = 0x20000;

	// required
	public static final int DDSCAPS_GENERIC_TEXTURE = 0x1000;

	public int flags, width, height;
	/**
	 * The pitch or number of bytes per scan line in an uncompressed texture;
	 * the total number of bytes in the top level texture for a compressed
	 * texture.
	 */
	public int pitchOrLinearSize;
	public int depth, mipMapCount;
	public final int[] reserved1 = new int[11];
	public int pxFmtFlags, rgbBitCount, rMask, gMask, bMask, aMask;

	public int capsGeneric, capsCubemap, capsReserved1, capsReserved2;
	public int reserved2;

	public ByteBuffer data;

	public String fourCCStr;

	public DDS_File(ByteBuffer b) throws IOException {
		if (!"DDS ".equals(read(b, 4)))
			throw new IOException("Bad magic");
		if (124 != b.getInt())
			throw new IOException("Bad structure size");
		flags = b.getInt();
		height = b.getInt();
		width = b.getInt();
		pitchOrLinearSize = b.getInt();
		depth = b.getInt();
		mipMapCount = b.getInt();
		ints(b, reserved1);
		if (b.getInt() != 32)
			throw new IOException();
		pxFmtFlags = b.getInt();
		if ((pxFmtFlags & DDPF_FOURCC) != DDPF_FOURCC)
			throw new IOException("Only block compressed DDS files are supported");
		fourCCStr = read(b, 4);
		rgbBitCount = b.getInt();
		rMask = b.getInt();
		gMask = b.getInt();
		bMask = b.getInt();
		aMask = b.getInt();
		capsGeneric = b.getInt();
		capsCubemap = b.getInt();
		capsReserved1 = b.getInt();
		capsReserved2 = b.getInt();
		reserved2 = b.getInt();

		int ol = b.limit();
		int len = dataLength();
		b.limit(b.position() + len);
		data = b.slice().order(b.order());
		b.limit(ol);
		b.position(b.position() + len);
	}

	public DDS_File(BufferedImage img, String format) {
		// writeImage handles flags, height, width, pitch, depth, mipmap counts, and
		// pxformat

		this.capsGeneric = DDSCAPS_GENERIC_TEXTURE;
		this.capsCubemap = this.capsReserved1 = this.capsReserved2 = 0;

		// just so writeImage doesn't fail
		this.data = ByteBuffer.allocate(1).order(ByteOrder.LITTLE_ENDIAN);
		writeImage(img, format);
	}

	@Override
	public void write(ByteBuffer b) throws IOException {
		write(b, 4, "DDS ");
		b.putInt(124); // header size
		b.putInt(flags);
		b.putInt(height);
		b.putInt(width);
		b.putInt(pitchOrLinearSize);
		b.putInt(depth);
		b.putInt(mipMapCount);
		writeInts(b, reserved1);
		b.putInt(32); // pxfmt size
		b.putInt(pxFmtFlags);
		write(b, 4, fourCCStr);
		b.putInt(rgbBitCount);
		b.putInt(rMask);
		b.putInt(gMask);
		b.putInt(bMask);
		b.putInt(aMask);
		b.putInt(capsGeneric);
		b.putInt(capsCubemap);
		b.putInt(capsReserved1);
		b.putInt(capsReserved2);
		b.putInt(reserved2);
		data.position(0);
		b.put(data);
	}

	@Override
	public int length() throws IOException {
		return 4 + 4 * 7 + 4 * reserved1.length + 4 * 2 + 4 + 4 * 10 + data.capacity();
	}

	private int dataLength() {
		return pitchOrLinearSize;
	}

	private int bytesPerChunk() {
		return (fourCCStr.equals("DXT1") ? 8 : 16);
	}

	private int chunksX() {
		return (int) Math.ceil(width / 4.0);
	}

	private int chunksY() {
		return (int) Math.ceil(height / 4.0);
	}

	private static final int RED_MASK = 0xFF0000;
	private static final int GREEN_MASK = 0xFF00;
	private static final int BLUE_MASK = 0xFF;
	private static final int[] RGB_MASKS = { RED_MASK, GREEN_MASK, BLUE_MASK };
	private static final int RGB_MASK = RED_MASK | GREEN_MASK | BLUE_MASK;
	private static final int ALPHA_MASK = 0xFF000000;

	private int unpackRGB565(short v) {
		return ALPHA_MASK | (((v >> 11) & 0x1F) << 19) | (((v >> 5) & 0x3F) << 10) | (((v >> 0) & 0x1F) << 3);
	}

	private short packRGB565(int argb) {
		int r = (argb & RED_MASK) >> 16;
		int g = (argb & GREEN_MASK) >> 8;
		int b = (argb & BLUE_MASK) >> 0;
		return (short) (((r >> 3) << 11) | ((g >> 2) << 5) | ((b >> 3) << 0));
	}

	private void fillAlphaTable(int[] alphaTable) {
		int a0 = alphaTable[0];
		int a1 = alphaTable[1];
		if (a0 > a1) {
			alphaTable[2] = (6 * a0 + 1 * a1) / 7;
			alphaTable[3] = (5 * a0 + 2 * a1) / 7;
			alphaTable[4] = (4 * a0 + 3 * a1) / 7;
			alphaTable[5] = (3 * a0 + 4 * a1) / 7;
			alphaTable[6] = (2 * a0 + 5 * a1) / 7;
			alphaTable[7] = (1 * a0 + 6 * a1) / 7;
		} else {
			alphaTable[2] = (4 * a0 + 1 * a1) / 5;
			alphaTable[3] = (3 * a0 + 2 * a1) / 5;
			alphaTable[4] = (2 * a0 + 3 * a1) / 5;
			alphaTable[5] = (1 * a0 + 4 * a1) / 5;
			alphaTable[6] = 0;
			alphaTable[7] = 0xFF;
		}
	}

	private void fillColorTable(int[] colorTable) {
		colorTable[2] = colorTable[3] = ALPHA_MASK;
		boolean embedAlpha = colorTable[0] <= colorTable[1];
		for (int mask : RGB_MASKS) {
			long v0 = colorTable[0] & (long) mask;
			long v1 = colorTable[1] & (long) mask;
			long v2, v3;
			if (!embedAlpha) {
				v2 = (2 * v0 + 1 * v1) / 3;
				v3 = (1 * v0 + 2 * v1) / 3;
			} else {
				v2 = (v0 + v1) / 2;
				v3 = 0;
			}
			if (v2 > mask)
				v2 = mask;
			if (v3 > mask)
				v3 = mask;
			colorTable[2] |= v2 & mask;
			colorTable[3] |= v3 & mask;
		}
		if (embedAlpha)
			colorTable[3] = 0;
	}

	private static double distRGB(int a, int b) {
		double dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
		double dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
		double db = ((a >> 0) & 0xFF) - ((b >> 0) & 0xFF);
		return Math.sqrt(dr * dr + dg * dg + db * db);
	}

	private double writeImageInternal(BufferedImage img, String fourCC) {
		if (!fourCC.equals("DXT3") && !fourCC.equals("DXT5"))
			throw new UnsupportedOperationException("Unsupported block compression: " + fourCC);
		double alphaError = 0;

		// setup header
		this.flags = DDSD_LINEARSIZE | DDSD_CAPS | DDSD_HEIGHT | DDSD_WIDTH | DDSD_PIXELFORMAT;
		this.height = img.getHeight();
		this.width = img.getWidth();
		// defer this until later: pitchOrLinearSize
		this.depth = 0;
		this.mipMapCount = 1;

		// setup pxfmt
		this.pxFmtFlags = DDPF_FOURCC;
		this.fourCCStr = fourCC;

		int bytesPerChunk = 16;
		int chunksX = chunksX();
		int chunksY = chunksY();
		this.pitchOrLinearSize = bytesPerChunk * chunksX * chunksY;
		this.data = ByteBuffer.allocate(this.pitchOrLinearSize).order(this.data.order());
		for (int bx = 0; bx < chunksX; bx++) {
			for (int by = 0; by < chunksY; by++) {
				int offset = bytesPerChunk * (bx + (by * chunksX));
				int offsetColor = offset + 8;

				long minRGB = 0xFFFFFFFF, maxRGB = 0;
				int[] colorsToWrite = new int[16];

				int px_x = bx * 4;
				int px_y = by * 4;
				for (int px = 0; px < 4; px++) {
					for (int py = 0; py < 4; py++) {
						int lookupI = px + (py * 4);
						if (px_x + px >= width || px_y + py >= height)
							colorsToWrite[lookupI] = colorsToWrite[lookupI - 1];
						long argb = colorsToWrite[lookupI] = img.getRGB(px_x + px, px_y + py);
						if ((argb & ALPHA_MASK) != 0)
							for (int mask : RGB_MASKS) {
								minRGB = (minRGB & ~mask) | Math.min(argb & mask, minRGB & mask);
								maxRGB = (maxRGB & ~mask) | Math.min(argb & mask, maxRGB & mask);
							}
					}
				}

				// packup the color values
				{
					int[] colorTable = new int[4];
					colorTable[0] = (int) maxRGB & RGB_MASK;
					colorTable[1] = (int) minRGB & RGB_MASK;
					fillColorTable(colorTable);
					data.putShort(offsetColor, packRGB565(colorTable[0]));
					data.putShort(offsetColor + 2, packRGB565(colorTable[1]));
					long colorLookupTable = 0;
					for (int i = 0; i < 16; i++) {
						int argb = colorsToWrite[i];
						int key = 0;
						for (int j = 0; j < 4; j++)
							if (distRGB(argb, colorTable[j]) < distRGB(argb, colorTable[key]))
								key = j;
						colorLookupTable |= (key & 3) << (i * 2);
					}
					data.putInt(offsetColor + 4, (int) colorLookupTable);
				}
				// packup the alpha values

				if (fourCC.equals("DXT3")) {
					for (int i = 0; i < 16; i += 2) {
						int a0 = (colorsToWrite[i] >> 24) & 0xFF;
						int a1 = (colorsToWrite[i + 1] >> 24) & 0xFF;
						alphaError += Math.abs(a0 - ((a0 >> 4) << 4));
						alphaError += Math.abs(a1 - ((a1 >> 4) << 4));
						byte val = (byte) (((a0 >> 4) & 0xF) | ((a1 >> 4) & 0xF));
						data.put(offset + (i / 2), val);
					}
				} else if (fourCC.equals("DXT5")) {
					int minAlpha = 0xFF, maxAlpha = 0;
					boolean hasBounds = false;
					for (int argb : colorsToWrite) {
						int a = (argb >> 24) & 0xFF;
						if (a == 0 || a == 0xFF)
							hasBounds |= true;
						else {
							minAlpha = Math.min(minAlpha, a);
							maxAlpha = Math.max(maxAlpha, a);
						}
					}
					int[] alphaTable = new int[8];
					if (hasBounds) {
						alphaTable[0] = minAlpha;
						alphaTable[1] = maxAlpha;
					} else {
						alphaTable[0] = maxAlpha;
						alphaTable[1] = minAlpha;
					}
					fillAlphaTable(alphaTable);
					long alphaLookupTable = 0;
					for (int i = 0; i < 16; i++) {
						int a = (colorsToWrite[i] >> 24) & 0xFF;
						int key = 0;
						for (int j = 0; j < 4; j++)
							if (Math.abs(a - alphaTable[j]) < Math.abs(a - alphaTable[key]))
								key = j;
						alphaError += Math.abs(a - alphaTable[key]);
						alphaLookupTable |= (key & 7) << (i * 3);
					}
					data.put(offset, (byte) alphaTable[0]);
					data.put(offset + 1, (byte) alphaTable[0]);
					data.putShort(offset + 2, (short) ((alphaLookupTable >> 0) & 0xFFFF));
					data.putShort(offset + 4, (short) ((alphaLookupTable >> 16) & 0xFFFF));
					data.putShort(offset + 6, (short) ((alphaLookupTable >> 32) & 0xFFFF));
				}
			}
		}
		return alphaError;
	}

	public void writeImage(BufferedImage img, String fourCC) {
		if (fourCC.equals("DXT35AUTO")) {
			double dxt3Error = writeImageInternal(img, "DXT3");
			double dxt5Error = writeImageInternal(img, "DXT5");
			if (dxt3Error < dxt5Error)
				writeImageInternal(img, "DXT3");
		} else
			writeImageInternal(img, fourCC);
	}

	public BufferedImage readImage() {
		BufferedImage out = new BufferedImage(width, height,
				(flags & DDPF_ALPHAPIXELS) != 0 ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
		if (fourCCStr == null || (pxFmtFlags & DDPF_FOURCC) == 0) {
			throw new UnsupportedOperationException("Uncompressed DDS");
		} else {
			int bytesPerChunk = bytesPerChunk();
			int chunksX = chunksX();
			int chunksY = chunksY();
			for (int bx = 0; bx < chunksX; bx++) {
				for (int by = 0; by < chunksY; by++) {
					int offset = bytesPerChunk * (bx + (by * chunksX));
					int offsetColor = offset + (fourCCStr.equals("DXT1") ? 0 : 8);
					int[] colorTable = new int[4];
					int c0 = data.getShort(offsetColor);
					int c1 = data.getShort(offsetColor + 2);
					colorTable[0] = unpackRGB565((short) c0);
					colorTable[1] = unpackRGB565((short) c1);
					fillColorTable(colorTable);

					long colorLookupTable = data.getInt(offsetColor + 4);

					int[] alphaTable = null;
					long alphaLookupTable = 0;
					if (fourCCStr.equals("DXT5")) {
						alphaTable = new int[8];
						alphaTable[0] = data.get(offset) & 0xFF;
						alphaTable[1] = data.get(offset + 1) & 0xFF;
						fillAlphaTable(alphaTable);
						for (int i = 0; i < 8; i++)
							alphaTable[i] = Math.min(0xFF, Math.max(0, alphaTable[i]));
						alphaLookupTable = ((data.getShort(offset + 2) & 0xFFFF) << 0L)
								| ((data.getShort(offset + 4) & 0xFFFF) << 16L)
								| ((data.getShort(offset + 6) & 0xFFFF) << 32L);
					}

					int px_x = bx * 4;
					int px_y = by * 4;
					for (int px = 0; px < 4; px++) {
						for (int py = 0; py < 4; py++) {
							if (px_x + px >= width || px_y + py >= height)
								continue;
							int lookupI = px + (py * 4);
							int argb = colorTable[(int) ((colorLookupTable >> (2 * lookupI)) & 3)];
							if (fourCCStr.equals("DXT5")) {
								argb = (argb & ~ALPHA_MASK);
								argb |= alphaTable[(int) ((alphaLookupTable >> (3 * lookupI)) & 7)] << 24;
							} else if (fourCCStr.equals("DXT3")) {
								argb = (argb & ~ALPHA_MASK);
								int piece = data.get(offset + (lookupI / 2)) & 0xFF;
								argb |= (piece >> ((lookupI & 1) * 4)) << 28;
							}
							out.setRGB(px_x + px, px_y + py, argb);
						}
					}
				}
			}
		}
		return out;
	}

}
