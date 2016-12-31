package repacker.texture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.Gen_IO;
import repacker.Utils;

public class TML_Texture extends Gen_IO {
	public final int textureID;
	public ByteBuffer data;

	public final int unknown1, unknown2, unknown4;
	public final short unknown3;

	public TML_Texture_Format format;
	public short width, height;

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

	public BufferedImage readImage() {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++) {
				int px = (y * width) + x;
				int argb = 0;
				switch (format) {
				case RGBA_4444: {
					int offset = 2 * px;
					int sv = ((data.get(offset) & 0xFF) << 8) | (data.get(offset + 1) & 0xFF);
					argb = (((sv >> 12) & 0xF) << 20) | (((sv >> 8) & 0xF) << 12) | (((sv >> 4) & 0xF) << 4)
							| (((sv >> 0) & 0xF) << 28);
					break;
				}
				case RGBA_5551: {
					int offset = 2 * px;
					int sv = ((data.get(offset) & 0xFF) << 8) | (data.get(offset + 1) & 0xFF);
					argb = (((sv >> 11) & 0x1F) << 19) | (((sv >> 6) & 0x1F) << 11) | (((sv >> 1) & 0x1F) << 3)
							| ((0xFF * (sv & 1)) << 24);
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
		return img;
	}

	public void writeDDS(File img, int width, int height) {
		this.format = TML_Texture_Format.RAW_DDS;
		this.width = (short) width;
		this.height = (short) height;
		this.data = Utils.read(img).order(this.data.order());
	}
	
	public void writeImage(BufferedImage img, boolean flipX, boolean flipY) {
		// skip the DDS thing
		this.format = TML_Texture_Format.RGBA_8888;
		this.width = (short) img.getWidth();
		this.height = (short) img.getHeight();
		this.data = ByteBuffer.allocate(4 * width * height).order(this.data.order());
		for (int y = 0; y < img.getHeight(); y++)
			for (int x = 0; x < img.getWidth(); x++) {
				int argb = img.getRGB(flipX ? (img.getWidth() - x - 1) : x, flipY ? (img.getHeight() - y - 1) : y);
				int i = ((y * width) + x) * 4;
				data.put(i, (byte) ((argb >> 16) & 0xFF));
				data.put(i + 1, (byte) ((argb >> 8) & 0xFF));
				data.put(i + 2, (byte) ((argb >> 0) & 0xFF));
				data.put(i + 3, (byte) ((argb >> 24) & 0xFF));
			}
	}
}
