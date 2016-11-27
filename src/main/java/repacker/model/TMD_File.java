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
		System.out.println(source + "\n" + header);
		int nodeStart = data.position();
		this.nodes = new TMD_Node_Block(this, data);
		int animStart = data.position();
		this.animations = new TMD_Animation_Block(this, data);
		int meshStart = data.position();
		this.meshes = new TMD_Mesh_Block(this, data);
		int eofStart = data.position();
		this.link();

		System.out.println("\tNodes:\t" + nodeStart);
		System.out.println("\tAnims:\t" + animStart + "\t" + (animStart - nodeStart));
		System.out.println("\tMeshs:\t" + meshStart + "\t" + (meshStart - animStart) + "\t" + (meshStart - nodeStart));
		System.out.println("\tEOF:\t" + eofStart + "\t" + (eofStart - meshStart) + "\t" + (eofStart - animStart) + "\t"
				+ (eofStart - nodeStart));
		System.out.println("\tEnd:\t" + data.capacity());
		System.out.println();
	}

	@Override
	public int length() throws IOException {
		int size = header.length();
		size = Math.max(size, header.nodeArrayOffset + nodes.length());
		size = Math.max(size, header.animationDataOffset + animations.length());
		size = Math.max(size, header.meshBlockOffset() + meshes.length());
		return size;
	}
	
	@Override
	public void write(ByteBuffer data) throws IOException {
		header.write(data);
		data.position(header.nodeArrayOffset);
		nodes.write(data);
		data.position(header.animationDataOffset);
		animations.write(data);
		data.position(header.meshBlockOffset());
		meshes.write(data);
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
