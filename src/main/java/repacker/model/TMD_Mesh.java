package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import repacker.Utils;

public class TMD_Mesh extends TMD_IO {
	public static class Vertex {
		public static final int SIZEOF = 4 * (3 + 3 + 2 + 2);

		public Vertex(ByteBuffer b) {
			position = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			normal = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			// skinningInfo = new Vector2(b.getFloat(), b.getFloat());
			b.get(skinningInfo);
			texpos = new Vector2(b.getFloat(), b.getFloat());
		}

		public final Vector3 position, normal;
		public final byte[] skinningInfo = new byte[8];

		public int[] bones;
		public float[] boneWeight;

		public final Vector2 texpos;
	}

	public final Vertex[] verts;
	// this seems to be a bone mapping, NOT a nodes-with-this-mesh mapping
	public final int[] meshParents;
	public final TMD_Node[] meshParentsRef;
	public int[] meshParentInverse;
	public final String material_name;
	public final short[] tri_strip;

	public final Vector3 boundingCenter, boundingExtents;

	private boolean loadedData;

	private final ByteBuffer vertex, index;

	public final byte[] unk1 = new byte[4];
	public final byte[] unk2 = new byte[8];
	public final byte[] unk3 = new byte[4];

	public int maxBindingsPerVertex;

	public boolean isSkinned() {
		return meshParents.length > 1;
	}

	public TMD_Mesh(TMD_File file, ByteBuffer b) throws UnsupportedEncodingException {
		super(file);
		b.get(unk1);
		int tri_strip_size = b.getInt();
		int num_verts = b.getInt();
		material_name = read(b, 32);
		b.get(unk2);
		int num_nodes = b.getInt();
		b.get(unk3);

		boundingCenter = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
		boundingExtents = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());

		meshParents = new int[num_nodes];
		meshParentsRef = new TMD_Node[num_nodes];
		for (int j = 0; j < meshParents.length; j++)
			meshParents[j] = b.getInt();
		verts = new Vertex[num_verts];
		tri_strip = new short[tri_strip_size];

		int vertexOffset = b.position();
		int indexOffset = vertexOffset + num_verts * Vertex.SIZEOF;
		int indexEnd = indexOffset + tri_strip_size * 2;

		b.position(vertexOffset);
		b.limit(indexOffset);
		this.vertex = b.slice();
		this.vertex.order(b.order());
		b.position(indexOffset);
		b.limit(indexEnd);
		this.index = b.slice();
		this.index.order(b.order());

		b.position(indexEnd);
		b.limit(b.capacity());
	}

	public void loadVtxAndTri() {
		if (loadedData)
			return;
		vertex.position(0);
		for (int i = 0; i < verts.length; i++)
			verts[i] = new Vertex(vertex);
		index.position(0);
		shorts(index, tri_strip);
		loadedData = true;
	}

	@Override
	public String toString() {
		return "MS[" + material_name + "]";
	}

	@Override
	public void link() {
		meshParentInverse = new int[file.scene.nodes.length];
		Arrays.fill(meshParentInverse, -1);
		for (int i = 0; i < meshParents.length; i++)
			meshParentInverse[meshParents[i]] = i;

		for (int i = 0; i < meshParents.length; i++)
			meshParentsRef[i] = file.scene.nodes[meshParents[i]];

		loadVtxAndTri();

		if (isSkinned()) {
			for (TMD_Mesh.Vertex v : verts) {
				int count = 0;
				while (count < 4 && v.skinningInfo[count] != 0)
					count++;
				v.bones = new int[count];
				v.boneWeight = new float[count];
				float totalWeight = 0;
				for (int i = 0; i < count; i++) {
					v.bones[i] = (v.skinningInfo[4 + i] & 0xFF) / 3;
					totalWeight += (v.boneWeight[i] = (v.skinningInfo[i] & 0xFF) / 255f);
				}
				if (totalWeight == 0 || v.bones.length == 0) {
					v.bones = new int[] { 0 };
					v.boneWeight = new float[] { 1 };
				}
			}

			maxBindingsPerVertex = 0;
			for (Vertex v : verts) {
				maxBindingsPerVertex = Math.max(maxBindingsPerVertex, v.bones.length);
			}
		}
	}
}
