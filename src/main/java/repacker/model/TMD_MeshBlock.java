package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_MeshBlock extends TMD_IO {
	public final byte[] unk1 = new byte[4];
	public final byte[] unk2 = new byte[4 * 5];
	public final TMD_Mesh[] meshes;

	public TMD_MeshBlock(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		int offs = 0x3C + data.getInt(0x1C) + 4;
		data.position(offs);
		data.get(unk1);
		int num_meshes = data.getInt();
		data.get(unk2);

		meshes = new TMD_Mesh[num_meshes];
		for (int i = 0; i < num_meshes; i++) {
			meshes[i] = new TMD_Mesh(file, data);
		}
	}

	@Override
	public void link() {
		for (TMD_Mesh m : meshes)
			m.link();
	}
}
