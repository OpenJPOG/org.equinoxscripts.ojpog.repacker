package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class TMD_Node_Array_Block extends TMD_IO {
	public final TMD_Node[] nodes;

	public TMD_Node_Array_Block(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(file.header.nodeArrayOffset);

		nodes = new TMD_Node[file.header.numNodes];
		for (int i = 0; i < file.header.numNodes; i++) {
			data.position(file.header.nodeArrayOffset + i * 0xB0);
			nodes[i] = new TMD_Node(this, data, i);
		}
	}

	@Override
	public void link() {
		for (TMD_Node node : nodes) {
			node.link();
		}
	}
}
