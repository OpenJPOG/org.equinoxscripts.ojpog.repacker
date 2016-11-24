package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;

import repacker.model.anim.TMD_Animation;
import repacker.model.ext.TKL_File;

public class TMD_File extends TMD_IO {
	public static final int SCENE_BLOCK_OFFSET = 0x40;

	public final String category;

	public final int sceneBlockSize;

	public int endOfScene() {
		return SCENE_BLOCK_OFFSET + sceneBlockSize;
	}

	/**
	 * Used to take the nodes listed in {@link TMD_Animation} and calculate the
	 * in-file position for each node's channel.
	 * 
	 * Exact math is: Read Start: lookupAddress + 60 - file.animOffsetOffset
	 */
	public final int rawMemoryOffset;

	public int rawOffsetToFile(int pos) {
		return pos + 60 - rawMemoryOffset;
	}

	public int fileOffsetToRaw(int real) {
		return real + rawMemoryOffset - 60;
	}

	public final byte[] unk2 = new byte[8];
	public final int fileLength;
	public final byte[] unk4 = new byte[8];

	public final TMD_Scene scene;
	public final TMD_MeshBlock meshes;

	public final String source;
	
	public final TMD_Header_Block header;

	public TMD_File(String k, ByteBuffer data) throws IOException {
		super(null);
		this.source = k;
		this.file = this;
		this.header = new TMD_Header_Block(this, data);
		
		data.position(4);
		zero(data, 4);
		fileLength = data.getInt();
		category = read(data, 8);
		data.get(unk4);
		sceneBlockSize = data.getInt();
		rawMemoryOffset = data.getInt();
		data.get(unk2);
		zero(data, 16);
		byte[] misc = new byte[SCENE_BLOCK_OFFSET - data.position()];
		data.get(misc);
		System.out.println(source + "\t" + ModelExtractor.hex(unk4) + "\t" + ModelExtractor.hex(misc) + "\t"
				+ ModelExtractor.hex(unk2));
		this.scene = new TMD_Scene(this, data);
		try {
			this.meshes = new TMD_MeshBlock(this, data);
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
		this.link();
	}

	public void write(ByteBuffer data) {
	}

	public TKL_File tklRepo;

	@Override
	public void link() {
		this.tklRepo = TKL_File.tkl(category + ".tkl");
		this.scene.link();
		this.meshes.link();
	}
}
