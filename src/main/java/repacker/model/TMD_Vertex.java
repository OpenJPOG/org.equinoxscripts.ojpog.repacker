package repacker.model;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import repacker.Utils;

public class TMD_Vertex {
	public static final int SIZEOF = 4 * (3 + 3 + 2 + 2);
	public final TMD_Mesh_Piece adder;

	public TMD_Vertex(TMD_Mesh_Piece piece, ByteBuffer b) {
		this.adder = piece;
		position = Utils.readV3(b);
		normal = Utils.readV3(b);
		b.get(skinningInfo);
		texpos = new Vector2(b.getFloat(), b.getFloat());
	}

	public final Vector3 position, normal;
	public final byte[] skinningInfo = new byte[8];

	public int[] bones;
	public TMD_Node[] bonesRef;
	public float[] boneWeight;

	public final Vector2 texpos;

	public TMD_Mesh_Piece user;
	public final Set<TMD_Mesh_Piece> users = new HashSet<>();

	public void usedBy(TMD_Mesh_Piece p) {
		if (users.add(p)) {
			// verify bone bindings don't change.
			if (bonesRef == null) {
				user = p;
				bonesRef = new TMD_Node[bones.length];
				for (int i = 0; i < bones.length; i++)
					bonesRef[i] = p.meshParentsRef[bones[i]];
			} else {
				for (int i = 0; i < bones.length; i++) {
					if (bonesRef[i] != p.meshParentsRef[bones[i]])
						System.err.println("Inconsistent binding state: Bad reference");
					if ((bones[i] % p.meshParents.length) != (bones[i] % user.meshParents.length))
						System.err.println("Inconsistent binding state: Won't be able to upload");
				}
			}
		}
	}

	private Map<TMD_Node, Float> weights = null;

	public Map<TMD_Node, Float> weights() {
		if (weights == null) {
			weights = new HashMap<>();
			for (int i = 0; i < bonesRef.length; i++)
				weights.put(bonesRef[i], boneWeight[i]);
			weights = Collections.unmodifiableMap(weights);
		}
		return weights;
	}

	public boolean weightsEqualEpsilon(TMD_Vertex other, float eps) {
		if (!weights().keySet().equals(other.weights().keySet()))
			return false;
		for (Entry<TMD_Node, Float> ft : weights().entrySet())
			if (Math.abs(ft.getValue() - other.weights().get(ft.getKey())) > eps)
				return false;
		return true;
	}
}