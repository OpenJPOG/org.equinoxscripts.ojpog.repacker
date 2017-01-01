package repacker.model.export;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import jassimp.AiBone;
import jassimp.AiBoneWeight;
import jassimp.AiMaterial;
import jassimp.AiMesh;
import jassimp.AiScene;
import jassimp.AiTextureType;
import repacker.model.TMD_File;
import repacker.model.mesh.TMD_Mesh;
import repacker.model.mesh.TMD_Mesh_Piece;
import repacker.model.mesh.TMD_Vertex;
import repacker.model.scene.TMD_Node;

public class FullMesh {
	public static class FullMeshVertex {
		public final Vector3 pos = new Vector3();
		public final Vector3 nrm = new Vector3();
		/**
		 * tex.y is always negative; this means "real" tex coord is 1+y
		 */
		public final Vector2 tex = new Vector2();
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
	public final Map<String, Integer> nodeToName = new HashMap<>();
	public final String[] nodes;

	private FullMesh(FullMeshVertex[] vtx, String id, String material, String[] nodes, int[] tris) {
		this.verts = vtx;
		this.materialName = material;
		this.mid = id;
		this.nodes = nodes;
		for (int i = 0; i < nodes.length; i++)
			this.nodeToName.put(nodes[i], i);
		this.tris = tris;
	}

	public FullMesh(TMD_Mesh mesh) {
		this.mid = "geom_" + mesh.hashCode();

		for (TMD_Mesh_Piece p : mesh.pieces)
			for (TMD_Node n : p.meshParentsRef)
				if (!nodeToName.containsKey(n.node_name))
					nodeToName.put(n.node_name, nodeToName.size());
		this.nodes = new String[nodeToName.size()];
		for (Entry<String, Integer> e : nodeToName.entrySet())
			this.nodes[e.getValue()] = e.getKey();

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
			for (Entry<TMD_Node, Float> w : baz.weights().entrySet())
				vt.weights.put(w.getKey().node_name, w.getValue());
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
		if (mesh.hasBones()) {
			this.nodes = new String[mesh.getBones().size()];
			for (int b = 0; b < nodes.length; b++) {
				AiBone bn = mesh.getBones().get(b);
				this.nodes[b] = bn.getName();
				this.nodeToName.put(bn.getName(), b);
				for (AiBoneWeight bw : bn.getBoneWeights())
					this.verts[bw.getVertexId()].weights.put(this.nodes[b], bw.getWeight());
			}
		} else {
			this.nodes = new String[0];
		}
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
		return new FullMesh(vtx, id, materialName, this.nodes, Arrays.copyOf(tris, triHead));
	}

	public int[] triStrip() {
		return new TriStripper_NV(this.tris).generate();
	}

	public TMD_Node[] nodes(TMD_File f) {
		TMD_Node[] map = new TMD_Node[this.nodes.length];
		for (int i = 0; i < map.length; i++)
			map[i] = f.nodes.byName(nodes[i]);
		return map;
	}

	public static final int TRI_STRIP_CAP = 7500;

	public TMD_Mesh export(TMD_File file) {
		if (verts.length >= Short.MAX_VALUE)
			throw new RuntimeException(
					"Maximum vertex count is " + Short.MAX_VALUE + ", this mesh has " + verts.length);
		TMD_Node[] bones = nodes(file);
		int[] meshParents = new int[bones.length];
		for (int i = 0; i < meshParents.length; i++)
			meshParents[i] = bones[i].id;
		TMD_Vertex[] vt = new TMD_Vertex[this.verts.length];
		int[] tii = triStrip();
		int pieceCount = (int) Math.ceil(tii.length / (double) TRI_STRIP_CAP);
		short[][] tiis = new short[pieceCount][TRI_STRIP_CAP + 50];
		TMD_Mesh_Piece[] pieces = new TMD_Mesh_Piece[pieceCount];
		System.out.println("Dividing into " + pieceCount + " pieces");
		for (int pi = 0; pi < pieceCount; pi++) {
			int toffset = pi * TRI_STRIP_CAP;
			int stripHead = 0;
			if (toffset > 0) {
				tiis[pi][stripHead++] = (short) tii[toffset - 2];
				tiis[pi][stripHead++] = (short) tii[toffset - 1];
			}
			for (int i = 0; i < Math.min(tii.length - toffset, TRI_STRIP_CAP); i++)
				tiis[pi][stripHead++] = (short) tii[toffset + i];
			System.out.println("Piece " + pi + " has " + stripHead + " tristrip");
			if (pi == 0)
				pieces[pi] = new TMD_Mesh_Piece(file, vt, Arrays.copyOf(tiis[pi], stripHead), meshParents);
			else
				pieces[pi] = new TMD_Mesh_Piece(file, vt.length, Arrays.copyOf(tiis[pi], stripHead), meshParents);
		}
		for (int i = 0; i < this.verts.length; i++) {
			Map<Integer, Float> nmap = new HashMap<>();
			for (Entry<String, Float> bind : this.verts[i].weights.entrySet())
				nmap.put(nodeToName.get(bind.getKey()), bind.getValue());
			vt[i] = new TMD_Vertex(pieces[0], this.verts[i].pos, this.verts[i].nrm, this.verts[i].tex, nmap);
		}
		for (TMD_Mesh_Piece p : pieces)
			p.computeBB(pieces[0].verts);
		return new TMD_Mesh(file, materialName, pieces);
	}
}