package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class TMD_Node extends TMD_IO {
	public TMD_Node(ByteBuffer b) throws UnsupportedEncodingException {
		this.quat = new Quaternion(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat());
		// skip identity of the transform
		skip(b, 12 * 4);
		this.translate = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
		// skip last float, then inverse transform
		skip(b, 17 * 4);
		byte len = b.get(); // length of node_name
		this.node_name = read(b, 15).substring(0, len & 0xFF);
		this.parent = b.getShort();
		this.unk1 = b.getShort();
		this.pivot_offset = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
	}

	public final Quaternion quat;
	public final Vector3 translate;
	public final String node_name;
	public final short parent;
	public final short unk1;
	public final Vector3 pivot_offset;
	public int scene_Unk1;

	@Override
	public String toString() {
		return node_name;
	}
}
