package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.model.anim.TMD_Animation_Block;
import repacker.model.ext.TKL_File;
import repacker.model.mesh.TMD_Mesh_Block;
import repacker.model.scene.TMD_Node_Block;

public class TMD_File extends TMD_IO {
	public final TMD_Mesh_Block meshes;

	public final String source;

	public final TMD_Header_Block header;
	public final TMD_Node_Block nodes;
	public final TMD_Animation_Block animations;

	public TMD_File(String k, ByteBuffer data) throws IOException {
		super(null);
		this.source = k;
		this.file = this;
		this.header = new TMD_Header_Block(this, data);
//		System.out.println(this.header);
		System.out.println(this.source + " " + hex(this.header.versionCode));
		this.nodes = new TMD_Node_Block(this, data);
		this.animations = new TMD_Animation_Block(this, data);
		this.meshes = new TMD_Mesh_Block(this, data);
		
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
