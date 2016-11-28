package repacker.model.mesh;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

import repacker.Utils;
import repacker.model.TMD_IO;
import repacker.model.scene.TMD_Node;

public class TMD_Mesh_Piece extends TMD_IO {
	// this seems to be a bone mapping, NOT a nodes-with-this-mesh mapping
	public final int[] meshParents;
	public final TMD_Node[] meshParentsRef;
	public int[] meshParentInverse;
	public final Vector3 boundingCenter, boundingExtents;

	private boolean loadedData;
	private final ByteBuffer vertex, index;
	public final TMD_Vertex[] verts;
	public final short[] tri_strip;
	public final TMD_Mesh root;

	public final int vertsRequired;

	public final int dataOffset, dataSize;

	public TMD_Mesh_Piece(TMD_Mesh root, ByteBuffer b) {
		super(root.file);
		this.dataOffset = b.position();

		int tris = b.getInt();
		int verts = b.getInt();
		int num_nodes = b.getInt();
		vertsRequired = b.getInt();

		boundingCenter = Utils.readV3(b);
		boundingExtents = Utils.readV3(b);

		if (num_nodes > 10e6 || tris > 10e6 || verts > 10e6)
			throw new RuntimeException("Bad mesh size " + num_nodes + ", " + tris + ", " + verts);
		meshParents = new int[num_nodes];
		meshParentsRef = new TMD_Node[num_nodes];
		for (int j = 0; j < meshParents.length; j++)
			meshParents[j] = b.getInt();

		this.root = root;
		this.verts = new TMD_Vertex[verts];
		this.tri_strip = new short[tris];

		int origLim = b.limit();

		int vertexOffset = b.position();
		int indexOffset = vertexOffset + verts * TMD_Vertex.SIZEOF;
		int indexEnd = indexOffset + tris * 2;
		b.position(vertexOffset);
		b.limit(indexOffset);
		this.vertex = b.slice();
		this.vertex.order(b.order());
		b.position(indexOffset);
		b.limit(indexEnd);
		this.index = b.slice();
		this.index.order(b.order());
		b.position(indexEnd);

		b.limit(origLim);

		dataSize = b.position() - dataOffset;
	}

	@Override
	public void write(ByteBuffer b) {
		b.putInt(this.tri_strip.length);
		b.putInt(this.verts.length);
		b.putInt(this.meshParents.length);
		b.putInt(vertsRequired);
		Utils.writeV3(b, boundingCenter);
		Utils.writeV3(b, boundingExtents);
		for (int j : meshParents)
			b.putInt(j);
		for (TMD_Vertex v : this.verts)
			v.write(b);
		for (short s : tri_strip)
			b.putShort(s);
	}

	@Override
	public int length() {
		int len = 4 + 4 + 4 + 4 + 12 + 12;
		len += 4 * meshParents.length;
		for (TMD_Vertex v : this.verts)
			len += v.length();
		len += 2 * tri_strip.length;
		return len;
	}

	public boolean isSkinned() {
		return true;
	}

	public void loadVtxAndTri() {
		if (loadedData)
			return;
		vertex.position(0);
		for (int i = 0; i < verts.length; i++)
			verts[i] = new TMD_Vertex(this, vertex);
		index.position(0);
		shorts(index, tri_strip);
		loadedData = true;
	}

	public int maxBindingsPerVertex;

	@Override
	public void link() {
		meshParentInverse = new int[file.nodes.nodes.length];
		Arrays.fill(meshParentInverse, -1);
		for (int i = 0; i < meshParents.length; i++)
			meshParentInverse[meshParents[i]] = i;

		for (int i = 0; i < meshParents.length; i++)
			meshParentsRef[i] = file.nodes.nodes[meshParents[i]];

		loadVtxAndTri();

		if (isSkinned()) {
			for (TMD_Vertex v : verts) {
				v.bindingsRawToID();
			}

			maxBindingsPerVertex = 0;
			for (TMD_Vertex v : verts) {
				maxBindingsPerVertex = Math.max(maxBindingsPerVertex, v.bones.length);
			}
		}
	}

	public void checkBoundingBox() {
		BoundingBox box = new BoundingBox().inf();
		for (TMD_Vertex v : verts)
			box.ext(v.position);
		Vector3 dims = box.getDimensions(new Vector3()).scl(0.5f);
		if (verts.length == 0)
			dims.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
		Vector3 center = box.getCenter(new Vector3());
		if (!center.epsilonEquals(boundingCenter, 1e-4f))
			System.err.println("Piece bounding centers inequal: " + center + " vs " + boundingCenter);
		if (!dims.epsilonEquals(boundingExtents, 1e-4f))
			System.err.println("Piece bounding extents inequal: " + dims + " vs " + boundingExtents);
	}
}
