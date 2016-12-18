package repacker.model.mesh;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_DLoD_Block extends TMD_IO {
	/**
	 * Levels. 0 is highest detail.
	 */
	public TMD_DLoD_Level[] levels;

	public final int[] variableHeader;

	public TMD_DLoD_Block(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(file.header.meshBlockOffset());
		int numLOD = data.getInt();
		ints(data, variableHeader = new int[1]);

		levels = new TMD_DLoD_Level[numLOD];
		for (int i = 0; i < levels.length; i++)
			levels[i] = new TMD_DLoD_Level(file, data);
	}

	@Override
	public int length() {
		int len = 4 + 4 * variableHeader.length;
		for (TMD_DLoD_Level g : levels)
			len += g.length();
		return len;
	}

	@Override
	public void write(ByteBuffer b) {
		b.putInt(levels.length);
		for (int i : variableHeader)
			b.putInt(i);
		for (TMD_DLoD_Level g : levels)
			g.write(b);
	}

	public int totalTris() {
		int i = 0;
		for (TMD_DLoD_Level m : levels)
			i += m.totalTris();
		return i;
	}

	public int totalVerts() {
		int i = 0;
		for (TMD_DLoD_Level m : levels)
			i += m.totalVerts();
		return i;
	}

	@Override
	public void link() {
		for (TMD_DLoD_Level m : levels)
			m.link();
	}
}
