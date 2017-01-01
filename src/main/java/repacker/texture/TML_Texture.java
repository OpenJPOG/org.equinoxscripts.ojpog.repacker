package repacker.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import repacker.Gen_IO;
import repacker.texture.dds.DDS_File;

public class TML_Texture extends Gen_IO {
	public final int textureID;
	private ByteBuffer data;

	public final int unknown1, unknown2, unknown4;
	public final short unknown3;

	private TML_Texture_Format format;
	public short width, height;

	public TML_Texture_Format format() {
		return format;
	}

	public TML_Texture(int id) {
		this.textureID = id;
		this.unknown1 = 0;
		this.unknown2 = 0;
		this.unknown3 = 0;
		this.unknown4 = 0;

		this.format = TML_Texture_Format.RGBA_8888;
		this.width = this.height = 1;
		this.data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
		this.data.putInt(0xFFFFFFFF);
		this.data.flip();
	}

	public TML_Texture(ByteBuffer b) {
		this.textureID = b.getInt();
		int dataSize = b.getInt();
		this.unknown1 = b.getInt();
		this.unknown2 = b.getInt();
		this.format = TML_Texture_Format.unpack(b.getShort());
		this.width = b.getShort();
		this.height = b.getShort();
		this.unknown3 = b.getShort();
		this.unknown4 = b.getInt();
		data = ByteBuffer.allocate(dataSize).order(b.order());
		int ol = b.limit();
		b.limit(b.position() + dataSize);
		data.put(b).flip();
		b.limit(ol);
		readCache = null;
		readDDSCache = null;
	}

	@Override
	public void write(ByteBuffer b) throws IOException {
		b.putInt(textureID);
		b.putInt(data.capacity());
		b.putInt(this.unknown1);
		b.putInt(this.unknown2);
		b.putShort(this.format.id);
		b.putShort(this.width);
		b.putShort(this.height);
		b.putShort(this.unknown3);
		b.putInt(this.unknown4);
		data.position(0);
		data.limit(data.capacity());
		b.put(data);
	}

	@Override
	public int length() throws IOException {
		return 4 * 4 + 2 * 4 + 4 + data.capacity();
	}

	private DDS_File readDDSCache = null;
	private BufferedImage readCache = null;

	public DDS_File readDDS() {
		if (format != TML_Texture_Format.RAW_DDS) {
			throw new IllegalStateException("Failed to read DDS file when format isn't DDS");
		}
		if (readDDSCache != null)
			return readDDSCache;
		data.position(0);
		try {
			return readDDSCache = new DDS_File(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedImage readImage() {
		if (readCache != null)
			return readCache;
		if (format == TML_Texture_Format.RAW_DDS)
			return readDDS().readImage();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				int px = (y * width) + x;
				int argb = 0;
				switch (format) {
				case ARGB_4444: {
					int offset = 2 * px;
					int sv = data.getShort(offset) & 0xFFFF;
					argb = (((sv >> 8) & 0xF) << 20) | (((sv >> 4) & 0xF) << 12) | (((sv >> 0) & 0xF) << 4)
							| (((sv >> 12) & 0xF) << 28);
					break;
				}
				case ARGB_1555: {
					int offset = 2 * px;
					int sv = data.getShort(offset) & 0xFFFF;
					argb = (((sv >> 0) & 0x1F) << 19) | (((sv >> 5) & 0x1F) << 11) | (((sv >> 10) & 0x1F) << 3)
							| ((0xFF * ((sv >> 15) & 1)) << 24);
					break;
				}
				case RGBA_8888: {
					int offset = 4 * px;
					argb = ((data.get(offset) & 0xFF) << 16) | ((data.get(offset + 1) & 0xFF) << 8)
							| ((data.get(offset + 2) & 0xFF) << 0) | ((data.get(offset + 3) & 0xFF) << 24);
					break;
				}
				case RAW_DDS:
				default:
					throw new UnsupportedOperationException();
				}
				img.setRGB(x, y, argb);
			}
		return readCache = img;
	}

	public void writeDDS(BufferedImage img, String ddsFormat) throws IOException {
		readCache = null;
		readDDSCache = null;
		this.format = TML_Texture_Format.RAW_DDS;
		this.width = (short) img.getWidth();
		this.height = (short) img.getHeight();
		DDS_File dds = new DDS_File(img, ddsFormat);
		this.data = ByteBuffer.allocate(dds.length()).order(ByteOrder.LITTLE_ENDIAN);
		dds.write(this.data);
		this.data.flip();
	}

	public void writeImage(TML_Texture_Format format, BufferedImage img) throws IOException {
		if (format == TML_Texture_Format.RAW_DDS) {
			writeDDS(img, "DXT35AUTO");
			return;
		}
		readCache = null;
		readDDSCache = null;
		// skip the DDS thing
		this.width = (short) img.getWidth();
		this.height = (short) img.getHeight();
		this.format = format;
		this.data = ByteBuffer.allocate(4 * width * height).order(this.data.order());
		for (int y = 0; y < img.getHeight(); y++)
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(x, y);
				int a = (argb >> 24) & 0xFF;
				int r = (argb >> 16) & 0xFF;
				int g = (argb >> 8) & 0xFF;
				int b = (argb >> 0) & 0xFF;

				int op = ((y * width) + x);
				switch (format) {
				case ARGB_4444: {
					int i = op * 2;
					int sv = 0;
					sv |= ((r >> 4) & 0xF) << 8;
					sv |= ((g >> 4) & 0xF) << 4;
					sv |= ((b >> 4) & 0xF) << 0;
					sv |= ((a >> 4) & 0xF) << 12;
					data.putShort(i, (short) sv);
					break;
				}
				case ARGB_1555: {
					int i = op * 2;
					int sv = ((a >> 7) & 1) << 15;
					sv |= ((r >> 3) & 0x1F) << 0;
					sv |= ((g >> 3) & 0x1F) << 5;
					sv |= ((b >> 3) & 0x1F) << 10;
					data.putShort(i, (short) sv);
					break;
				}
				case RGBA_8888: {
					int i = op * 4;
					data.put(i, (byte) r);
					data.put(i + 1, (byte) g);
					data.put(i + 2, (byte) b);
					data.put(i + 3, (byte) a);
					break;
				}
				case RAW_DDS:
				default:
					throw new UnsupportedOperationException();
				}
			}
	}
}
