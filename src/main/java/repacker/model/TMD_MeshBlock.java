package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_MeshBlock extends TMD_IO {
	public final TMD_Mesh_Group[] meshes;

	public final int[] variableHeader;

	public TMD_MeshBlock(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(0x40 + file.header.sceneBlockSize - 4);
		int numMeshes = data.getInt();

		System.out.println(file.source + " mesh begins at: " + data.position());
		ints(data, variableHeader = new int[1]);

		meshes = new TMD_Mesh_Group[numMeshes];
		for (int i = 0; i < meshes.length; i++)
			meshes[i] = new TMD_Mesh_Group(file, data);
	}

	@Override
	public void link() {
		for (TMD_Mesh_Group m : meshes)
			m.link();
	}

	public void write(ByteBuffer data) {
	}
}
