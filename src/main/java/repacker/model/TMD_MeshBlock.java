package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_MeshBlock extends TMD_IO {
	public final TMD_Mesh[] meshes;
	public final int unk1;

	public TMD_MeshBlock(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		int offs = TMD_File.SCENE_BLOCK_OFFSET + file.sceneBlockSize;
		data.position(offs);
		// This isn't always there: what is it?
		this.unk1 = data.getInt();
		System.err.println(ModelExtractor.pad(Integer.toBinaryString(unk1), 32));

		meshes = new TMD_Mesh[file.scene.meshCount];
		for (int i = 0; i < meshes.length; i++) {
			int s = data.position();
			try {
				meshes[i] = new TMD_Mesh(file, data);
			} catch (Exception e) {
				System.err.println("Failed to read mesh #" + i + " at offset=" + s + " (block offset is " + offs + ")");
				if (e instanceof RuntimeException)
					throw (RuntimeException) e;
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void link() {
		for (TMD_Mesh m : meshes)
			m.link();
	}
}
