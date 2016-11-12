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

	private final boolean useBoneGlow = false;
	private final boolean useNearestBone = false;
	private final boolean useEmbedded = true;

	@Override
	public void link() {
		meshParentInverse = new int[file.scene.nodes.length];
		Arrays.fill(meshParentInverse, -1);
		for (int i = 0; i < meshParents.length; i++)
			meshParentInverse[meshParents[i]] = i;

		for (int i = 0; i < meshParents.length; i++)
			meshParentsRef[i] = file.scene.nodes[meshParents[i]];

		loadVtxAndTri();

		// Generate bones if possible
		Set<TMD_Mesh.Vertex> fixed = new HashSet<>();

		for (TMD_Mesh.Vertex v : verts) {
			if (fixed.contains(v))
				continue;
			if (meshParentsRef.length == 1) {
				v.bones = new int[] { 0 };
				v.boneWeight = new float[] { 1 };
			} else if (useBoneGlow) {
				// Nodes with equal positions must have identical weighting.
				Set<TMD_Mesh.Vertex> mine = new HashSet<>();
				Vector3 bulkPosition = new Vector3();
				Vector3 bulkNormal = new Vector3();
				for (TMD_Mesh.Vertex v2 : verts) {
					if (v.position.epsilonEquals(v2.position, 1e-2f)) {
						mine.add(v2);
						bulkNormal.add(v.normal);
						bulkPosition.add(v.position);
					}
				}
				bulkPosition.scl(1f / mine.size());
				bulkNormal.scl(1f / mine.size());

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
							rd.set(bulkPosition).sub(p);
							float limit = rd.len();
							rd.nor();
							// float lambert = rd.dot(bulkNormal) / (limit *
							// limit);
							// if (lambert < 0)
							// continue;
							boolean hit = false;
							Ray ray = new Ray(p, rd);
							for (int i = 0; i < tri_strip.length - 2; i++) {
								Vertex va = verts[tri_strip[i]];
								Vertex vb = verts[tri_strip[i + 1]];
								Vertex vc = verts[tri_strip[i + 2]];
								if (va != vb && vb != vc && vc != va && Intersector.intersectRayTriangle(ray,
										va.position, vb.position, vc.position, out)) {
									float d = out.dst(ray.origin);
									if (d > 0.5f && d < limit) {
										hit = true;
										break;
									}
								}
							}
							if (!hit) {
								float transmit = new Vector3(rd).crs(dir).len() / boneLen;
								float c = transmit / (limit * limit);
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
				v.bones = new int[] { (int) sts[0][0], (int) sts[1][0] };
				float pbw = (float) sts[0][1] + 1;
				float sbw = (float) sts[1][1] + 1;
				v.boneWeight = new float[] { pbw / (pbw + sbw), sbw / (pbw + sbw) };

				fixed.addAll(mine);
				for (TMD_Mesh.Vertex vp : mine) {
					vp.bones = v.bones;
					vp.boneWeight = v.boneWeight;
				}
			} else if (useNearestBone) {
				int bestNode = -1;
				float bestDist = Float.MAX_VALUE;
				int secondBestNode = -1;
				float secondBestDist = Float.MAX_VALUE;

				for (int b = 0; b < meshParents.length; b++) {
					if (meshParentsRef[b].parentRef != null) {
						Vector3 start = meshParentsRef[b].worldPosition.getTranslation(new Vector3());
						Vector3 end = meshParentsRef[b].parentRef.worldPosition.getTranslation(new Vector3());
						Vector3 cd = Utils.nearestSegmentPoint(start, end, v.position);
						cd.sub(v.position);
						float len = cd.len();
						cd.scl(1 / len);
						float crs = cd.crs(end.sub(start).nor()).len();
						len *= 5 + crs;
						if (len < secondBestDist) {
							secondBestDist = len;
							secondBestNode = b;
						}
						if (secondBestDist < bestDist) {
							int tn = bestNode;
							float td = bestDist;
							bestDist = secondBestDist;
							bestNode = secondBestNode;
							secondBestDist = td;
							secondBestNode = tn;
						}
					}
				}
				v.bones = new int[] { bestNode, secondBestNode };
				float pbw = (float) Math.exp(-bestDist);
				float sbw = (float) Math.exp(-secondBestDist);
				v.boneWeight = new float[] { pbw / (pbw + sbw), sbw / (pbw + sbw) };
			} else if (useEmbedded) {
				// System.out.println(ModelExtractor.hex(v.skinningInfo));
				// laid out as 4 weights, 4 bones.
				int count = 0;
				while (count < 4 && v.skinningInfo[count] != 0)
					count++;
				v.bones = new int[count];
				v.boneWeight = new float[count];
				for (int i = 0; i < count; i++) {
					v.bones[i] = (v.skinningInfo[4 + i] & 0xFF) / 3;
					v.boneWeight[i] = (v.skinningInfo[i] & 0xFF) / 255f;
				}
			}
		}

		maxBindingsPerVertex = 0;
		for (Vertex v : verts) {
			maxBindingsPerVertex = Math.max(maxBindingsPerVertex, v.bones.length);
		}
	}
}
