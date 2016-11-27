package repacker.model.mesh;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_Mesh_Group extends TMD_IO {
	public final TMD_Mesh[] members;

	public final int unk1;
	public final float[] unk2 = new float[4];

	public TMD_Mesh_Group(TMD_File file, ByteBuffer b) throws UnsupportedEncodingException {
		super(file);
		this.members = new TMD_Mesh[b.getInt()];
		this.unk1 = b.getInt();
		floats(b, unk2);
		for (int i = 0; i < members.length; i++)
			members[i] = new TMD_Mesh(this, b);
	}

	@Override
	public void write(ByteBuffer b) {
		b.putInt(members.length);
		b.putInt(unk1);
		for (float f : unk2)
			b.putFloat(f);
		for (TMD_Mesh m : members)
			m.write(b);
	}

	@Override
	public int length() {
		int len = 4 + 4 + 4 * unk2.length;
		for (TMD_Mesh m : members)
			len += m.length();
		return len;
	}

	public int totalTris() {
		int i = 0;
		for (TMD_Mesh m : members)
			i += m.totalTris;
		return i;
	}

	public int totalVerts() {
		int i = 0;
		for (TMD_Mesh m : members)
			i += m.verts.length;
		return i;
	}

	@Override
	public void link() {
		for (TMD_Mesh m : members)
			m.link();
	}
}
