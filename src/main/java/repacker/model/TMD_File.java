package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.model.ext.TKL_File;

public class TMD_File extends TMD_IO {
	public final TMD_MeshBlock meshes;

	public final String source;

	public final TMD_Header_Block header;
	public final TMD_Node_Block nodes;
	public final TMD_Animation_Block animations;

	public TMD_File(String k, ByteBuffer data) throws IOException {
		super(null);
		this.source = k;
		this.file = this;
		this.header = new TMD_Header_Block(this, data);
		this.nodes = new TMD_Node_Block(this, data);
		this.animations = new TMD_Animation_Block(this, data);
		this.meshes = new TMD_MeshBlock(this, data);
		
		this.link();
	}

	public void write(ByteBuffer data) {
	}

	public TKL_File tklRepo;

	@Override
	public void link() {
		this.tklRepo = TKL_File.tkl(header.category + ".tkl");

		this.header.link();
		this.nodes.link();
		this.animations.link();
		this.meshes.link();
	}
}
