package repacker.model.mesh;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import repacker.Utils;
import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_Mesh extends TMD_IO {
	public final String material_name;

	public final TMD_Mesh_Piece[] pieces;

	public final int totalTriStripLength;

	public final TMD_Vertex[] verts;

	public TMD_Mesh(TMD_File file, String material, TMD_Mesh_Piece[] pieces) {
		super(file);
		this.material_name = material;
		this.pieces = pieces;
		int ttl = 0;
		int ttv = 0;
		for (TMD_Mesh_Piece p : pieces) {
			ttl += p.tri_strip.length;
			ttv += p.verts.length;
		}
		this.totalTriStripLength = ttl;
		this.verts = new TMD_Vertex[ttv];
	}

	public TMD_Mesh(TMD_DLoD_Level file, ByteBuffer b) throws UnsupportedEncodingException {
		super(file.file);
		int pieceCount = b.getInt();
		totalTriStripLength = b.getInt();
		int totalVerts = b.getInt();

		material_name = read(b, 32);
		// check material name (debugging)
		if (!Utils.isPermitted(material_name))
			throw new RuntimeException("Not permissible: \"" + material_name + "\"");

		this.verts = new TMD_Vertex[totalVerts];

		pieces = new TMD_Mesh_Piece[pieceCount];
		for (int i = 0; i < pieces.length; i++)
			pieces[i] = new TMD_Mesh_Piece(this, b);
	}

	@Override
	public void write(ByteBuffer b) {
		b.putInt(pieces.length);
		b.putInt(totalTriStripLength);
		b.putInt(verts.length);
		write(b, 32, material_name);
		for (TMD_Mesh_Piece p : pieces)
			p.write(b);
	}

	@Override
	public int length() {
		int len = 4 + 4 + 4 + 32;
		for (TMD_Mesh_Piece m : pieces)
			len += m.length();
		return len;
	}

	@Override
	public String toString() {
		return "MS[" + material_name + " t=" + totalTriStripLength + " v=" + (verts == null ? "null" : "" + verts.length) + "]";
	}

	public void loadVtxAndTri() {
		int offset = 0;
		for (TMD_Mesh_Piece p : pieces) {
			p.loadVtxAndTri();
			System.arraycopy(p.verts, 0, verts, offset, p.verts.length);
			offset += p.verts.length;
		}
	}

	public int maxBindingsPerVertex;
	private boolean isSkinned;

	public boolean isSkinned() {
		return isSkinned;
	}

	@Override
	public void link() {
		for (TMD_Mesh_Piece p : pieces)
			p.link();
		loadVtxAndTri();

		isSkinned = false;
		maxBindingsPerVertex = 0;
		for (TMD_Mesh_Piece p : pieces) {
			isSkinned |= p.isSkinned();
			maxBindingsPerVertex = Math.max(maxBindingsPerVertex, p.maxBindingsPerVertex);
			for (short s : p.tri_strip)
				verts[s].usedBy(p);
		}
		for (int i = 0; i < verts.length; i++)
			if (verts[i].user == null)
				System.out.println("No user " + i);

		// Integrity check thing. Not needed in production.
		Set<TMD_Vertex> check = new HashSet<>();
		for (TMD_Vertex va : verts) {
			if (!check.add(va))
				continue;
			Set<TMD_Vertex> similar = new HashSet<>();
			for (TMD_Vertex vb : verts)
				if (va != vb && vb.position.epsilonEquals(va.position, 1e-4f))
					similar.add(vb);

			for (TMD_Vertex vb : similar) {
				if (!va.weightsEqualEpsilon(vb, 1e-2f)) {
					System.err.println("Split vertex not weighted equally");
				}
			}
			check.addAll(similar);
		}
		for (TMD_Mesh_Piece p : pieces)
			p.checkBoundingBox();
	}
}