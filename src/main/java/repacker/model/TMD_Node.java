package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class TMD_Node extends TMD_IO {
	public TMD_Node(ByteBuffer b) throws UnsupportedEncodingException {
		Quaternion rotation = new Quaternion(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat());
		Matrix4 worldMat = new Matrix4(floats(b, new float[16]));
		Matrix4 invWorldMat = new Matrix4(floats(b, new float[16]));

		byte len = b.get(); // length of node_name (in theory)
		this.node_name = read(b, 15);
		this.parent = b.getShort();
		this.noMesh = b.getShort();
		this.worldPosition = new Matrix4().set(new Vector3(b.getFloat(), b.getFloat(), b.getFloat()), rotation);
	}

	public final Matrix4 worldPosition;
	public final String node_name;
	public final short parent;
	/**
	 * <em>1</em> if there is no mesh attached to me or any decendents.
	 */
	public final short noMesh;

	public int scene_Unk1;
	public TMD_Node parentRef;
	public TMD_Node[] childRef = new TMD_Node[0];

	@Override
	public String toString() {
		return node_name;
	}
}
