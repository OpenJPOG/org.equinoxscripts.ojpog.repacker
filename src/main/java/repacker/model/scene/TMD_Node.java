package repacker.model.scene;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import repacker.Utils;
import repacker.model.TMD_IO;

public class TMD_Node extends TMD_IO {
	private final TMD_Node_Block scene;

	public TMD_Node(TMD_Node_Block block, ByteBuffer b, int id) throws UnsupportedEncodingException {
		super(block.file);
		this.scene = block;
		this.id = id;
		Quaternion rotation = Utils.readQ(b);
		this.worldSkinningMatrix = new Matrix4(floats(b, new float[16]));
		this.worldSkinningMatrix_Inv = new Matrix4(floats(b, new float[16]));

		@SuppressWarnings("unused")
		byte len = b.get(); // length of node_name (in theory)
		this.node_name = read(b, 15);
		this.parent = b.getShort();
		this.noMesh = bool(b.getShort());
		// unknown
		this.matrix2 = new Matrix4().set(Utils.readV3(b), rotation);

		// if we are skinned...
		this.worldPosition = worldSkinningMatrix;
		this.worldPosition_Inv = worldSkinningMatrix_Inv;

		this.localPosition = new Matrix4();
		this.localPosition_Inv = new Matrix4();

		this.localSkinningMatrix = new Matrix4();
		this.localSkinningMatrix_Inv = new Matrix4();
	}

	@Override
	public int length() {
		int len = 4 * 4;
		len += 4 * 16;
		len += 4 * 16;
		len += 1 + 15;
		len += 4;
		len += 4 * 3;
		return len;
	}

	@Override
	public void write(ByteBuffer b) {
		Utils.writeQ(b, matrix2.getRotation(new Quaternion()));
		for (float f : worldSkinningMatrix.val)
			b.putFloat(f);
		for (float f : worldSkinningMatrix_Inv.val)
			b.putFloat(f);
		b.put((byte) node_name.length());
		write(b, 15, node_name);
		b.putShort(this.parent);
		b.putShort((short) (this.noMesh ? 1 : 0));
		Utils.writeV3(b, this.matrix2.getTranslation(new Vector3()));
	}

	public final int id;
	public final Matrix4 worldPosition, worldPosition_Inv;

	public final Matrix4 localPosition, localPosition_Inv;

	public final Matrix4 worldSkinningMatrix, worldSkinningMatrix_Inv;
	public final Matrix4 localSkinningMatrix, localSkinningMatrix_Inv;
	public final Matrix4 matrix2;

	public final String node_name;
	public final short parent;
	/**
	 * <em>1</em> if there is no mesh attached to me or any decendents.
	 */
	public final boolean noMesh;

	public TMD_Node parentRef;
	public TMD_Node[] childRef = new TMD_Node[0];

	@Override
	public String toString() {
		return node_name;
	}

	@Override
	public void link() {
		this.localPosition.set(worldPosition);
		this.localSkinningMatrix.set(worldSkinningMatrix);

		if (parent >= 0) {
			parentRef = scene.nodes[parent];
			parentRef.childRef = Arrays.copyOf(parentRef.childRef, parentRef.childRef.length + 1);
			parentRef.childRef[parentRef.childRef.length - 1] = this;

			this.localPosition.mulLeft(parentRef.worldPosition_Inv);
			this.localSkinningMatrix.mulLeft(parentRef.worldSkinningMatrix_Inv);
		}
		this.localPosition_Inv.set(localPosition).inv();
		this.localSkinningMatrix_Inv.set(localSkinningMatrix).inv();
	}
}
