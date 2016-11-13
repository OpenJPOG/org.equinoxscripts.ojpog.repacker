package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.Utils;
import repacker.model.ext.TKL_File;

public class TMD_File extends TMD_IO {
	public static final int SCENE_BLOCK_OFFSET = 0x40;

	public final String category;
	/**
	 * Always 0x5849?
	 */
	public final short cst1;
	/**
	 * Always 0x07a80032, except sometimes 0x00880023 on skinned things.
	 */
	public final int acst2;
	/**
	 * Always zero
	 */
	public final short zer1;

	public final int sceneBlockSize;
	public final int[] unk2 = new int[3];
	public final int[] zer2 = new int[4];

	public final TMD_Scene scene;
	public final TMD_MeshBlock meshes;

	public TMD_File(String k, ByteBuffer data) throws IOException {
		super(null);
		this.file = this;
		if (!read(data, 4).equals("TMDL"))
			throw new IOException("Bad magic");
		data.position(12);
		category = read(data, 8);
		// Read some header info:
		data.position(20);
		cst1 = data.getShort();
		acst2 = data.getInt();
		zer1 = data.getShort();
		sceneBlockSize = data.getInt();
		ints(data, unk2);
		ints(data, zer2);

		this.scene = new TMD_Scene(this, data);
		try {
			this.meshes = new TMD_MeshBlock(this, data);
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}

		if (data.hasRemaining())
			System.out.println("File ends at " + data.capacity() + ", read ends at " + data.position() + ", remaining="
					+ data.remaining());
		this.link();
	}

	public TKL_File tklRepo;

	@Override
	public void link() {
		this.tklRepo = TKL_File.tkl(category + ".tkl");
		this.scene.link();
		this.meshes.link();
	}
}
