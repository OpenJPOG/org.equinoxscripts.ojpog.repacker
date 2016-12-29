package repacker.model;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.Utils;
import repacker.model.anim.TMD_Animation;
import repacker.model.anim.TMD_Animation_Block;
import repacker.model.ext.TKL_File;
import repacker.model.mesh.TMD_DLoD_Block;
import repacker.model.mesh.TMD_Mesh;
import repacker.model.scene.TMD_Node_Block;

public class TMD_File extends TMD_IO {
	public final TMD_DLoD_Block dLoD;

	public final String source;

	public final TMD_Header_Block header;
	public final TMD_Node_Block nodes;
	public final TMD_Animation_Block animations;

	public TMD_File(File f) throws IOException {
		this(f, Utils.read(f));
	}

	public TMD_File(File f, ByteBuffer data) throws IOException {
		super(null);
		this.source = f.getName().substring(0, f.getName().length() - 4);
		this.file = this;
		this.header = new TMD_Header_Block(this, data);
		// System.out.println(source + "\n" + header);
		int nodeStart = data.position();
		this.nodes = new TMD_Node_Block(this, data);
		int animStart = data.position();
		this.animations = new TMD_Animation_Block(this, data);
		int meshStart = data.position();
		this.dLoD = new TMD_DLoD_Block(this, data);
		int eofStart = data.position();

		this.tklRepo = new TKL_File(Utils.read(new File(f.getParentFile(), this.header.category + ".tkl")));
		this.link();
	}

	@Override
	public int length() throws IOException {
		int size = header.length();
		size = Math.max(size, header.nodeArrayOffset + nodes.length());
		size = Math.max(size, header.animationDataOffset + animations.length());
		size = Math.max(size, header.meshBlockOffset() + dLoD.length());
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
		dLoD.write(data);
	}

	public String summary() {
		StringBuilder sb = new StringBuilder();
		sb.append("Nodes: ").append("\n-").append(this.nodes.sceneGraph(a -> "").replace("\n", "\n-")).append("\n");
		sb.append("Animations:").append("\n");
		for (TMD_Animation a : animations.animations)
			sb.append("-" + a.name + ": " + a.length + " sec").append("\n");
		sb.append("Meshes:").append("\n");
		for (TMD_Mesh m : dLoD.levels[0].members)
			sb.append("-Mesh mat=" + m.material_name + ", v=" + m.verts + ", t=" + m.totalTriStripLength + ", pieces="
					+ m.pieces).append("\n");
		return sb.toString();
	}

	public void updateIntegrity() throws IOException {
		header.fileLength = length() - 12;
	}

	public final TKL_File tklRepo;

	@Override
	public void link() {
		this.header.link();
		this.nodes.link();
		this.animations.link();
		this.dLoD.link();
	}
}
