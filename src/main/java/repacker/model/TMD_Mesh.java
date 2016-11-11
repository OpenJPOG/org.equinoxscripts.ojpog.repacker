package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

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

		public int primaryBone, secondaryBone;
		public float primaryBoneAlpha;

		public final Vector2 texpos;
	}

	public final Vertex[] verts;
	// this seems to be a bone mapping, NOT a nodes-with-this-mesh mapping
	public final int[] meshParents;
	public final TMD_Node[] meshParentsRef;
	public final String material_name;
	public final short[] tri_strip;

	public final Vector3 boundingCenter, boundingExtents;

	private boolean loadedData;

	private final ByteBuffer vertex, index;

	public final byte[] unk1 = new byte[4];
	public final byte[] unk2 = new byte[8];
	public final byte[] unk3 = new byte[4];

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
		for (int i = 0; i < meshParents.length; i++)
			meshParentsRef[i] = file.scene.nodes[meshParents[i]];

		loadVtxAndTri();

		// Generate bones if possible
		for (TMD_Mesh.Vertex v : verts) {
			if (meshParentsRef.length == 1 || 0<1) {
				v.secondaryBone = v.primaryBone = meshParentsRef[0].id;
				v.primaryBoneAlpha = 1;
			} else {
				// calculate bone weight using bone glow. Creds to Rich Wareham
				// and Joan Lasenby
				Object[][] sts = new Object[meshParents.length][2];
				for (int b = 0; b < meshParents.length; b++) {
					sts[b][0] = meshParents[b];
					float w = 0;
					if (meshParentsRef[b].parentRef != null) {
						Vector3 start = meshParentsRef[b].worldPosition.getTranslation(new Vector3());
						Vector3 dir = meshParentsRef[b].parentRef.worldPosition.getTranslation(new Vector3())
								.sub(start);
						Vector3 p = new Vector3();
						Vector3 rd = new Vector3();
						Vector3 out = new Vector3();
						float boneLen = dir.len();
						for (float t = 0; t <= 1; t += 0.1f) {
							p.set(start).mulAdd(dir, t);
							rd.set(v.position).sub(p);
							float limit = rd.len();
							rd.nor();
							float lambert = rd.dot(v.normal) / (limit * limit);
							if (lambert < 0)
								continue;
							float transmit = new Vector3(rd).crs(dir).len() / boneLen;
							boolean hit = false;
							Ray ray = new Ray(p, rd);
							for (int i = 0; i < tri_strip.length - 2; i++) {
								Vertex va = verts[tri_strip[i]];
								Vertex vb = verts[tri_strip[i+1]];
								Vertex vc = verts[tri_strip[i+2]];
								if (va != vb && vb != vc && vc != va && Intersector.intersectRayTriangle(ray,
										va.position, vb.position, vc.position, out)) {
									float d = out.dst(ray.origin);
									if (d > 0 && d < limit) {
										hit = true;
										break;
									}
								}
							}
							if (!hit) {
								float c = lambert * transmit;
								w += c;
							}
						}
					}
					sts[b][1] = w;
				}
				Arrays.sort(sts, new Comparator<Object[]>() {
					@Override
					public int compare(Object[] o1, Object[] o2) {
						return -Float.compare((float) o1[1], (float) o2[1]);
					}
				});
				// two bones per vertex.
				v.primaryBone = (int) sts[0][0];
				float pbw = (float) sts[0][1] + 1;
				v.secondaryBone = (int) sts[1][0];
				float sbw = (float) sts[1][1] + 1;
				v.primaryBoneAlpha = pbw / (pbw + sbw);
			}
		}
		System.exit(0);
	}
}
