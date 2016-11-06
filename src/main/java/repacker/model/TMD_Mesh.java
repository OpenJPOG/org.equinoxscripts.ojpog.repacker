package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class TMD_Mesh {
	public static class Vertex {
		public static final int SIZEOF = 4 * (3 + 3 + 2 + 2);

		public Vertex(ByteBuffer b) {
			position = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			normal = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			skinningInfo = new Vector2(b.getFloat(), b.getFloat());
			texpos = new Vector2(b.getFloat(), b.getFloat());
		}

		public final Vector3 position, normal;
		public final Vector2 skinningInfo, texpos;
	}

	private final Vertex[] verts;
	// this seems to be a bone mapping, NOT a nodes-with-this-mesh mapping
	private final int[] nodes;
	private final String material_name;
	private final short[] tri_strip;

	public TMD_Mesh(ByteBuffer b) throws UnsupportedEncodingException {
		byte[] unknown1 = bytes(b, 4);
		int tri_strip_size = b.getInt();
		int num_verts = b.getInt();
		material_name = read(b, 32);
		byte[] unknown2 = bytes(b, 4 + 4);
		int num_nodes = b.getInt();
		byte[] unknown3 = bytes(b, 4);

		Vector3 boundingCenter = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
		Vector3 boundingExtents = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());

		nodes = new int[num_nodes];
		for (int j = 0; j < nodes.length; j++)
			nodes[j] = b.getInt();
		verts = new Vertex[num_verts];
		for (int i = 0; i < num_verts; i++)
			verts[i] = new Vertex(b);
		tri_strip = new short[tri_strip_size];
		for (int j = 0; j < tri_strip.length; j++)
			tri_strip[j] = b.getShort();

		// System.out.println(
		// "Mesh " + material_name + " " + hex(unknown1) + " | " +
		// hex(unknown2) + " | " + hex(unknown3));
	}

	@Override
	public String toString() {
		return "MS[" + material_name + "]";
	}
}
