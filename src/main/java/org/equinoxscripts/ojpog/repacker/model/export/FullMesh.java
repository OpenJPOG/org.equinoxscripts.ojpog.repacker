package org.equinoxscripts.ojpog.repacker.model.export;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_Mesh;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_Mesh_Piece;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_Vertex;

import jassimp.AiBone;
import jassimp.AiBoneWeight;
import jassimp.AiMaterial;
import jassimp.AiMesh;
import jassimp.AiScene;
import jassimp.AiTextureType;

public class FullMesh {
	public static class FullMeshVertex {
		public final Vector3f pos = new Vector3f();
		public final Vector3f nrm = new Vector3f();
		/**
		 * tex.y is always negative; this means "real" tex coord is 1+y
		 */
		public final Vector2f tex = new Vector2f();
		public final Map<String, Float> weights = new HashMap<>();

		public boolean epsEquals(FullMeshVertex o) {
			if (!pos.epsilonEquals(o.pos, 0.01f))
				return false;
			if (!nrm.epsilonEquals(o.nrm, 0.001f))
				return false;
			if (!tex.epsilonEquals(o.tex, 0.001f))
				return false;
			if (weights.size() != o.weights.size())
				return false;
			for (Entry<String, Float> k : weights.entrySet()) {
				Float lk = o.weights.get(k.getKey());
				if (lk == null || Math.abs(lk.floatValue() - k.getValue().floatValue()) >= 0.001f)
					return false;
			}
			return true;
		}
	}

	public final String mid;

	public final FullMesh.FullMeshVertex[] verts;
	public final String materialName;
	public final int[] tris;

	private FullMesh(FullMeshVertex[] vtx, String id, String material, int[] tris) {
		this.verts = vtx;
		this.materialName = material;
		this.mid = id;
		this.tris = tris;
	}

	public FullMesh(TMD_Mesh mesh) {
		this.mid = "geom_" + mesh.hashCode();
		this.materialName = mesh.material_name;
		short[] remap = new short[mesh.verts.length];
		Arrays.fill(remap, (short) -1);
		List<FullMesh.FullMeshVertex> vts = new ArrayList<>(mesh.verts.length);
		main: for (short i = 0; i < mesh.verts.length; i++) {
			TMD_Vertex baz = mesh.verts[i];
			for (short j = 0; j < vts.size(); j++) {
				if (vts.get(j).pos.epsilonEquals(baz.position, 1e-3f)
						&& vts.get(j).tex.epsilonEquals(baz.texpos, 1e-2f)) {
					remap[i] = j;
					continue main;
				}
			}
			remap[i] = (short) vts.size();
			FullMesh.FullMeshVertex vt = new FullMeshVertex();
			vt.pos.set(baz.position);
			vt.nrm.set(baz.normal);
			vt.tex.set(baz.texpos);
			{
				Map<Integer, Float> bs = baz.weightsBoneID();
				for (Entry<Integer, Float> w : bs.entrySet()) {
					int nodeID = baz.user.meshParents[w.getKey().intValue()];
					String nam = baz.user.file.nodes.nodes[nodeID].node_name;
					vt.weights.put(nam, w.getValue());
				}
			}
			vts.add(vt);
		}
		this.verts = vts.toArray(new FullMesh.FullMeshVertex[vts.size()]);

		int th = 0;
		int[] tris = new int[mesh.totalTriStripLength * 3];
		for (TMD_Mesh_Piece p : mesh.pieces) {
			int j = 0;
			tris: for (int i = 2; i < p.tri_strip.length; i++) {
				short a, b, c;
				if ((j++ & 1) == 0) {
					a = remap[p.tri_strip[i - 2]];
					b = remap[p.tri_strip[i - 1]];
					c = remap[p.tri_strip[i - 0]];
				} else {
					a = remap[p.tri_strip[i - 0]];
					b = remap[p.tri_strip[i - 1]];
					c = remap[p.tri_strip[i - 2]];
				}
				if (a == b || b == c || c == a)
					continue;
				// Repeat?
				for (int r = 0; r < th - 2; r += 3) {
					int a0 = tris[r];
					int b0 = tris[r + 1];
					int c0 = tris[r + 2];
					if (a == a0 && b == b0 && c == c0)
						continue tris;
					else if (a == b0 && b == c0 && c == a0)
						continue tris;
					else if (a == c0 && b == a0 && c == b0)
						continue tris;
					else if (a == a0 && b == c0 && c == b0)
						continue tris;
					else if (a == b0 && b == a0 && c == c0)
						continue tris;
					else if (a == c0 && b == b0 && c == a0)
						continue tris;
				}
				// Write
				tris[th++] = a;
				tris[th++] = b;
				tris[th++] = c;
			}
		}
		this.tris = Arrays.copyOf(tris, th);
	}

	public FullMesh(AiScene scene, AiMesh mesh) {
		this.mid = mesh.getName().trim().isEmpty() ? "geom_" + mesh.hashCode() : mesh.getName();
		this.verts = new FullMeshVertex[mesh.getNumVertices()];
		for (int i = 0; i < this.verts.length; i++) {
			FullMeshVertex v = this.verts[i] = new FullMeshVertex();
			v.pos.set(mesh.getPositionX(i), mesh.getPositionY(i), mesh.getPositionZ(i));
			v.nrm.set(mesh.getNormalX(i), mesh.getNormalY(i), mesh.getNormalZ(i));
			v.tex.set(mesh.getTexCoordU(i, 0), mesh.getTexCoordV(i, 0));
		}
		if (mesh.hasBones())
			for (AiBone bn : mesh.getBones())
				for (AiBoneWeight bw : bn.getBoneWeights())
					this.verts[bw.getVertexId()].weights.put(bn.getName(), bw.getWeight());
		IntBuffer idx = mesh.getIndexBuffer();
		this.tris = new int[idx.capacity()];
		for (int j = 0; j < this.tris.length; j++)
			this.tris[j] = idx.get();
		AiMaterial mtl = scene.getMaterials().get(mesh.getMaterialIndex());
		String mat = new File(mtl.getTextureFile(AiTextureType.DIFFUSE, 0)).getName();
		// Strip extension
		int lk = mat.indexOf('.');
		if (lk != -1)
			mat = mat.substring(0, lk);
		// Strip _0
		if (mat.endsWith("_0"))
			mat = mat.substring(0, mat.length() - 2);
		this.materialName = mat;
	}

	/**
	 * Merges identical vertices
	 */
	public FullMesh clean() {
		int[] rootRemap = new int[verts.length];
		int[] vertexRemap = new int[verts.length];
		int remapSize = 0;

		Arrays.fill(vertexRemap, -1);
		for (int v : tris) {
			if (vertexRemap[v] != -1)
				continue;
			vertexRemap[v] = remapSize;
			rootRemap[remapSize] = v;
			// find similar
			for (int j : tris)
				if (verts[v].epsEquals(verts[j]))
					vertexRemap[j] = remapSize;
			remapSize++;
		}

		FullMeshVertex[] vtx = new FullMeshVertex[remapSize];
		for (int i = 0; i < remapSize; i++)
			vtx[i] = this.verts[rootRemap[i]];
		String id = mid + "_clean";
		int[] tris = new int[this.tris.length];
		int triHead = 0;
		for (int i = 0; i < tris.length; i += 3) {
			int a = vertexRemap[this.tris[i]];
			int b = vertexRemap[this.tris[i + 1]];
			int c = vertexRemap[this.tris[i + 2]];
			if (a == b || b == c || c == a)
				continue;
			tris[triHead++] = a;
			tris[triHead++] = b;
			tris[triHead++] = c;
		}
		return new FullMesh(vtx, id, materialName, Arrays.copyOf(tris, triHead));
	}

	public static final int TRI_STRIP_CAP = 7500;
	public static final int BONE_COUNT_CAP = 28;

	public Map<String, Integer> requiredNodeNames() {
		Map<String, Integer> nodeToName = new HashMap<>();
		for (FullMeshVertex v : this.verts)
			for (String s : v.weights.keySet())
				if (!nodeToName.containsKey(s))
					nodeToName.put(s, nodeToName.size());
		return Collections.unmodifiableMap(nodeToName);
	}

	private class PieceBuilder {
		private final Map<String, Integer> bones = new HashMap<>();

		private class PossibleAdd {
			private final int[] tri;

			private PossibleAdd(int[] tri) {
				this.tri = tri;
			}

			private int nb;

			private void computeAddFactor() {
				int nbs = 0;
				for (int t : tri)
					for (String s : verts[t].weights.keySet())
						if (!bones.containsKey(s))
							nbs++;
				this.nb = nbs;
			}
		}

		private final Set<int[]> triQueue;

		private int[] tri;
		private int triHead;

		public PieceBuilder(Set<int[]> triQueue) {
			this.triQueue = triQueue;

			this.tri = new int[triQueue.size() * 3];
			this.triHead = 0;

			List<PossibleAdd> include = new ArrayList<>(triQueue.size());
			for (Iterator<int[]> itr = triQueue.iterator(); itr.hasNext();)
				include.add(new PossibleAdd(itr.next()));

			add(include.get(0).tri);
			include.remove(0);
			while (!include.isEmpty()) {
				include.forEach(a -> a.computeAddFactor());
				include.sort((a, b) -> Integer.compare(a.nb, b.nb));
				int ots = include.get(0).nb;
				if (bones.size() + ots > BONE_COUNT_CAP)
					break;
				for (Iterator<PossibleAdd> itr = include.iterator(); itr.hasNext();) {
					PossibleAdd test = itr.next();
					if (test.nb != ots)
						break;
					add(test.tri);
					itr.remove();
				}
			}
			this.tri = Arrays.copyOf(tri, triHead);
		}

		private void add(int[] tri) {
			if (!this.triQueue.remove(tri))
				System.err.println("Unwrap failure");
			this.tri[triHead++] = tri[0];
			this.tri[triHead++] = tri[1];
			this.tri[triHead++] = tri[2];
			for (int t : tri)
				for (String s : verts[t].weights.keySet())
					if (!bones.containsKey(s))
						bones.put(s, bones.size());
		}
	}

	public TMD_Mesh export(TMD_File file) {
		if (verts.length >= Short.MAX_VALUE)
			throw new RuntimeException(
					"Maximum vertex count is " + Short.MAX_VALUE + ", this mesh has " + verts.length);
		TMD_Vertex[] vt = new TMD_Vertex[this.verts.length];
		List<PieceBuilder> primaryPieces = new ArrayList<>();
		{
			Set<int[]> triangles = new HashSet<>();
			for (int i = 0; i < this.tris.length; i += 3)
				triangles.add(new int[] { tris[i], tris[i + 1], tris[i + 2] });
			while (!triangles.isEmpty())
				primaryPieces.add(new PieceBuilder(triangles));
		}
		List<TMD_Mesh_Piece> pieces = new ArrayList<>();
		System.out.println("Divided into " + primaryPieces.size() + " meta pieces due to bone complexity");
		for (PieceBuilder piece : primaryPieces) {
			int[] meshParents = new int[piece.bones.size()];
			for (Entry<String, Integer> e : piece.bones.entrySet())
				meshParents[e.getValue()] = file.nodes.byName(e.getKey()).id;

			int[] tii = new TriStripper_NV(piece.tri).generate();
			int pieceCount = (int) Math.ceil(tii.length / (double) TRI_STRIP_CAP);
			System.out.println("Dividing piece into " + pieceCount
					+ " pieces for triangle count reasons (bone count is " + meshParents.length + ")");
			for (int pi = 0; pi < pieceCount; pi++) {
				int toffset = pi * TRI_STRIP_CAP;
				int stripHead = 0;
				short[] tiis = new short[TRI_STRIP_CAP + 50];
				if (toffset > 0) {
					tiis[stripHead++] = (short) tii[toffset - 2];
					tiis[stripHead++] = (short) tii[toffset - 1];
				}
				for (int i = 0; i < Math.min(tii.length - toffset, TRI_STRIP_CAP); i++)
					tiis[stripHead++] = (short) tii[toffset + i];
				System.out.println("Piece " + pi + " has " + stripHead + " tristrip");
				if (pieces.isEmpty())
					pieces.add(new TMD_Mesh_Piece(file, vt, Arrays.copyOf(tiis, stripHead), meshParents));
				else
					pieces.add(new TMD_Mesh_Piece(file, vt.length, Arrays.copyOf(tiis, stripHead), meshParents));
			}
			for (int v : piece.tri) {
				if (vt[v] != null)
					continue;
				Map<Integer, Float> nmap = new HashMap<>();
				for (Entry<String, Float> bind : this.verts[v].weights.entrySet())
					nmap.put(piece.bones.get(bind.getKey()), bind.getValue());
				vt[v] = new TMD_Vertex(this.verts[v].pos, this.verts[v].nrm, this.verts[v].tex, nmap);
			}
		}
		for (TMD_Mesh_Piece p : pieces)
			p.computeBB(pieces.get(0).verts);
		return new TMD_Mesh(file, materialName, pieces.toArray(new TMD_Mesh_Piece[pieces.size()]));
	}
}