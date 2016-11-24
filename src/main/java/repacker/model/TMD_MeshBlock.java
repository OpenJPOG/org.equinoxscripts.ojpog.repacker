package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_MeshBlock extends TMD_IO {
	public final TMD_Mesh_Group[] meshes;

	public final int[] variableHeader;

	public TMD_MeshBlock(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(file.endOfScene());
		System.out.println(file.source + " mesh begins at: " + data.position());
		ints(data, variableHeader = new int[1]);

		meshes = new TMD_Mesh_Group[file.scene.meshCount];
		for (int i = 0; i < meshes.length; i++) {
			try {
				meshes[i] = new TMD_Mesh_Group(file, data);
			} catch (Exception e) {
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void link() {
		for (TMD_Mesh_Group m : meshes)
			m.link();
	}

	public void write(ByteBuffer data) {
	}
}
