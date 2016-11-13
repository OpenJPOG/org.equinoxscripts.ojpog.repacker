package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

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
	public void link() {
		for (TMD_Mesh m : members)
			m.link();
	}
}
