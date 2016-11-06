package repacker.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TMD_File extends TMD_IO {
	public final String category;
	public final long unkCategory;
	public final byte[] unk1 = new byte[4];
	public final byte[] unk2 = new byte[3 * 4];
	public final byte[] unk3 = new byte[4 * 4];
	
	public final TMD_Scene scene;

	public TMD_File(ByteBuffer data) {
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

		// Read Scene Graph Block
		short numNodes = data.getShort(0x40);
		data.position(0x44);
		short unknown1 = data.getShort();
		short sceneFeatures = data.getShort();
		skip(data, 42); // zeros
		short unknown2 = data.getShort();
		short unknown3 = data.getShort();
		Node[] nodes = new Node[numNodes];
		for (int i = 0; i < numNodes; i++) {
			data.position(0x7C + i * 0xB0);
			nodes[i] = new Node(data);
		}
		data.position(0x7C + numNodes * 0xB0);
		int[] unknown4 = ints(data, 9);
		int[] nodeUnknown = ints(data, numNodes);

		// Read Mesh Block
		int offs = 0x3C + data.getInt(0x1C) + 8;
		data.position(offs - 4);
		byte[] mesh_block_unk1 = bytes(data, 4);
		data.position(offs);
		int num_meshes = data.getInt();
		byte[] mesh_block_unk2 = bytes(data, 4 * 5);
		// System.out.println(hex(test1) + "\t" + hex(test));
		Mesh[] meshes = new Mesh[num_meshes];
		for (int i = 0; i < num_meshes; i++) {
			meshes[i] = new Mesh(data);
			// System.out.println(f.getName() + "\t" + i + "\t" +
			// Arrays.stream(meshes[i].nodes)
			// .mapToObj(a -> nodes[a]).map(a ->
			// a.node_name).collect(Collectors.toList()));
		}
	}
}
