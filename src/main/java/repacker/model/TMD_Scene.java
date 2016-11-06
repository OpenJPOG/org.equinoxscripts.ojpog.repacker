package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_Scene extends TMD_IO {
	public final byte[] unk1 = new byte[2];
	public final short sceneFeatures;
	public final byte[] unk2_Zero = new byte[42];
	public final byte[] unk3 = new byte[4];
	public final int[] unk4 = new int[9];
	public final TMD_Node[] nodes;

	public TMD_Scene(ByteBuffer data) throws UnsupportedEncodingException {
		short numNodes = data.getShort(0x40);
		data.position(0x44);
		data.get(unk1);
		sceneFeatures = data.getShort();
		data.get(unk2_Zero);
		data.get(unk3);
		nodes = new TMD_Node[numNodes];
		for (int i = 0; i < numNodes; i++) {
			data.position(0x7C + i * 0xB0);
			nodes[i] = new TMD_Node(data);
		}
		data.position(0x7C + numNodes * 0xB0);
		ints(data, unk4);
		for (int i = 0; i < numNodes; i++) {
			nodes[i].scene_Unk1 = data.getInt();
		}
	}
}
