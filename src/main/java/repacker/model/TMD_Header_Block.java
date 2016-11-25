package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

public class TMD_Header_Block extends TMD_IO {
	public final String category;

	public final int sceneBlockSize;
	public final int rawMemoryOffset;

	public int rawOffsetToFile(int pos) {
		return pos + 60 - rawMemoryOffset;
	}

	public int fileOffsetToRaw(int real) {
		return real + rawMemoryOffset - 60;
	}

	public final byte[] unk2 = new byte[8];
	public final int fileLength;
	public final byte[] unk4 = new byte[4];
	public final byte[] unk3 = new byte[4];

	public final int versionCode;
	public final int numNodes;
	public final int numAnimations;
	public final int numS1, numS2;

	public final int nodeHeaderOffset, animationDataOffset, nodeArrayOffset;

	public static final Set<Integer> versions = new HashSet<>();

	public TMD_Header_Block(TMD_File file, ByteBuffer data) throws IOException {
		data.position(0);

		if (!read(data, 4).equals("TMDL"))
			throw new IOException("Bad magic");

		zero(data, 4);
		fileLength = data.getInt(); // remaining
		if (data.remaining() != fileLength)
			System.err.println(fileLength + " vs " + data.remaining());

		category = read(data, 8);
		versionCode = data.getInt();
		versions.add(versionCode);
		data.get(unk4);
		sceneBlockSize = data.getInt();
		rawMemoryOffset = data.getInt();
		data.get(unk2);
		zero(data, 16);
		data.get(unk3);
		numNodes = data.getShort() & 0xFFFF;
		numS1 = data.getShort() & 0xFFFF;
		numAnimations = data.getShort() & 0xFFFF;
		numS2 = data.getShort() & 0xFFFF;
		zero(data, 44);

		nodeHeaderOffset = rawOffsetToFile(data.getInt());
		if (nodeHeaderOffset == 0x7C) {
			nodeArrayOffset = nodeHeaderOffset;
			animationDataOffset = rawOffsetToFile(data.getInt());
		} else {
			nodeArrayOffset = rawOffsetToFile(data.getInt());
			animationDataOffset = rawOffsetToFile(data.getInt());
		}

		// ?? TODO
		if (nodeHeaderOffset != nodeArrayOffset) {
			data.position(nodeHeaderOffset);
			int[] order = new int[numNodes];
			ints(data, order);
		}
	}

	@Override
	public String toString() {
		return "Header[\n\tVersion=" + versionCode + "\n\tNodes=" + numNodes + " @ " + hex(nodeArrayOffset)
				+ "\n\tAnims=" + numAnimations + " @ " + hex(animationDataOffset) + "\n\tunk2=" + hex(unk2)
				+ "\n\tunk3=" + hex(unk3) + "\n\tunk4=" + hex(unk4) + "\n\tnumS1=" + numS1 + "\n\tnumS2=" + numS2
				+ "\n]";
	}
}
