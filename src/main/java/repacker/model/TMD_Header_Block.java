package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TMD_Header_Block extends TMD_IO {
	public static final long VERSION_MAGIC_A = 584117868617L;
	public static final long VERSION_MAGIC_B = 8418139199561L;

	public final String category;

	public final int sceneBlockSize;
	public final int rawMemoryOffset;

	public int rawOffsetToFile(int pos) {
		return pos + 60 - rawMemoryOffset;
	}

	public int fileOffsetToRaw(int real) {
		return real + rawMemoryOffset - 60;
	}

	// Tends to be a low number for flora, vehicles, and misc.
	// Tends to be a high number for dinos
	public final int unk2_1;
	public final int unk2_2;

	public final int fileLength;

	/**
	 * 584117868617 or 8418139199561
	 */
	public final long versionMagic;

	public final int numNodes;
	public final int numAnimations;
	public final boolean[] numS1;
	public final boolean[] numS2;

	public final int nodeHeaderOffset, animationDataOffset, nodeArrayOffset;

	public int meshBlockOffset() {
		return 0x40 + sceneBlockSize - 4;
	}

	public TMD_Header_Block(TMD_File file, ByteBuffer data) throws IOException {
		data.position(0);

		if (!read(data, 4).equals("TMDL"))
			throw new IOException("Bad magic");

		zero(data, 4);
		fileLength = data.getInt(); // remaining
		if (data.remaining() != fileLength)
			System.err.println(fileLength + " vs " + data.remaining());

		category = read(data, 8);
		versionMagic = data.getLong();
		if (versionMagic != VERSION_MAGIC_A && versionMagic != VERSION_MAGIC_B)
			throw new IOException("Unknown version magic");

		sceneBlockSize = data.getInt();
		rawMemoryOffset = data.getInt();
		unk2_1 = data.getInt();
		unk2_2 = data.getInt();
		zero(data, 16);
		int sceneBlockSize2 = data.getInt();
		if (sceneBlockSize2 != sceneBlockSize)
			throw new IOException("Scene block size mismatch");
		numNodes = data.getShort() & 0xFFFF;
		numS1 = bitfield(data.getShort(), 7);
		numAnimations = data.getShort() & 0xFFFF;
		numS2 = bitfield(data.getShort(), 4);
		zero(data, 44);

		nodeHeaderOffset = rawOffsetToFile(data.getInt());
		if (nodeHeaderOffset == 0x7C) {
			nodeArrayOffset = nodeHeaderOffset;
			animationDataOffset = rawOffsetToFile(data.getInt());
		} else {
			nodeArrayOffset = rawOffsetToFile(data.getInt());
			animationDataOffset = rawOffsetToFile(data.getInt());
		}

		/**
		 * TODO Seems associated when {@link #versionMagic} isn't A or B.
		 */
		if (nodeHeaderOffset != nodeArrayOffset) {
			data.position(nodeHeaderOffset);
			int[] order = new int[numNodes];
			ints(data, order);
		}
	}

	@Override
	public void write(ByteBuffer b) throws IOException {
		write(b, 4, "TMDL");
		writeZero(b, 4);
		b.putInt(fileLength);
		write(b, 8, category);
		b.putLong(versionMagic);
		b.putInt(sceneBlockSize);
		b.putInt(rawMemoryOffset);
		b.putInt(unk2_1);
		b.putInt(unk2_2);
		writeZero(b, 16);
		b.putInt(sceneBlockSize);
		b.putShort((short) numNodes);
		b.putShort((short) bitfield(numS1));
		b.putShort((short) numAnimations);
		b.putShort((short) bitfield(numS2));
		writeZero(b, 44);
		b.putInt(fileOffsetToRaw(nodeHeaderOffset));
		if (nodeHeaderOffset == 0x7C) {
			b.putInt(fileOffsetToRaw(animationDataOffset));
		} else {
			b.putInt(fileOffsetToRaw(nodeArrayOffset));
			b.putInt(fileOffsetToRaw(animationDataOffset));
		}

		if (nodeHeaderOffset != nodeArrayOffset) {
			throw new IOException("Unsupported");
		}
	}

	@Override
	public int length() throws IOException {
		int size = 0;
		size += 4 + 4; // magic, zero
		size += 8; // cat
		size += 8; // version magic
		size += 4; // scene block size
		size += 4; // raw memory offset
		size += 4; // unk2_1
		size += 4; // unk2_2
		size += 16; // zero
		size += 4; // scene block size 2
		size += 2 + 2 + 2 + 2; // nums
		size += 44; // zero
		size += 4 + 4; // offsets
		if (nodeHeaderOffset != 0x7C)
			size += 4; // extra
		if (nodeHeaderOffset != nodeArrayOffset)
			size += numNodes * 4;
		return size;
	}

	@Override
	public String toString() {
		return "Header[\n\tNodes=" + numNodes + " @ " + hex(nodeArrayOffset) + "\n\tAnims=" + numAnimations + " @ "
				+ hex(animationDataOffset) + "\n\tunk2_1=" + unk2_1 + "\n\tunk2_2=" + unk2_2 + "\n\tnumS1="
				+ Arrays.toString(numS1) + "\n\tnumS2=" + Arrays.toString(numS2) + "\n]";
	}
}
