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
		data.position(0x40 + file.header.sceneBlockSize - 4);
		int numMeshes = data.getInt();
		System.out.println("Meshes=" + numMeshes + " @ " + hex(data.position()) + "\t"
				+ hex(file.header.fileOffsetToRaw(data.position())));

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
