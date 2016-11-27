package repacker.model.mesh;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_Mesh_Block extends TMD_IO {
	public final TMD_Mesh_Group[] meshes;

	public final int[] variableHeader;

	public TMD_Mesh_Block(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(file.header.meshBlockOffset());
		int numMeshes = data.getInt();
		ints(data, variableHeader = new int[1]);

		meshes = new TMD_Mesh_Group[numMeshes];
		for (int i = 0; i < meshes.length; i++)
			meshes[i] = new TMD_Mesh_Group(file, data);
	}

	@Override
	public int length() {
		int len = 4 + 4;
		for (TMD_Mesh_Group g : meshes)
			len += g.length();
		return len;
	}

	@Override
	public void write(ByteBuffer b) {
		b.putInt(meshes.length);
		for (int i : variableHeader)
			b.putInt(i);
		for (TMD_Mesh_Group g : meshes)
			g.write(b);
	}

	public int totalTris() {
		int i = 0;
		for (TMD_Mesh_Group m : meshes)
			i += m.totalTris();
		return i;
	}

	public int totalVerts() {
		int i = 0;
		for (TMD_Mesh_Group m : meshes)
			i += m.totalVerts();
		return i;
	}

	@Override
	public void link() {
		for (TMD_Mesh_Group m : meshes)
			m.link();
	}
}
