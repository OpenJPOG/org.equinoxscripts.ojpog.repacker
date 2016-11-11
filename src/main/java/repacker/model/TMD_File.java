package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.model.ext.TKL_File;

public class TMD_File extends TMD_IO {
	public final String category;
	public final long unkCategory;
	public final byte[] unk1 = new byte[4];
	public final byte[] unk2 = new byte[3 * 4];
	public final byte[] unk3 = new byte[4 * 4];

	public final TMD_Scene scene;
	public final TMD_MeshBlock meshes;

	public TMD_File(ByteBuffer data) throws IOException {
		super(null);
		this.file = this;
		if (!read(data, 4).equals("TMDL"))
			throw new IOException("Bad magic");
		data.position(12);
		category = read(data, 8);
		// Read some header info:
		data.position(20);
		// 1 value per category except with structures
		unkCategory = data.getLong();
		data.get(unk1);
		data.get(unk2);
		data.get(unk3);

		this.scene = new TMD_Scene(this);
		this.scene.load(data);
		this.meshes = new TMD_MeshBlock(this, data);

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
