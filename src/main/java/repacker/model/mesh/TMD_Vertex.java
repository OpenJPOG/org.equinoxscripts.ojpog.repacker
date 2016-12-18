package repacker.model.mesh;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import repacker.Utils;
import repacker.model.TMD_IO;
import repacker.model.scene.TMD_Node;

public class TMD_Vertex extends TMD_IO {
	public static final int SIZEOF = 4 * (3 + 3 + 2 + 2);
	public final TMD_Mesh_Piece adder;

	public TMD_Vertex(TMD_Mesh_Piece piece, ByteBuffer b) {
		this.adder = piece;
		position = Utils.readV3(b);
		normal = Utils.readV3(b);
		b.get(skinningInfo);
		texpos = new Vector2(b.getFloat(), b.getFloat());
	}

	public TMD_Vertex(TMD_Mesh_Piece p, Vector3 pos, Vector3 nrm, Vector2 tex, Map<Integer, Float> weights) {
		this.adder = p;
		this.position = pos;
		this.normal = nrm;
		this.texpos = tex;
		List<Entry<Integer, Float>> binds = new ArrayList<>(weights.entrySet());
		binds.sort((a, b) -> -Float.compare(a.getValue(), b.getValue()));
		int cnt = Math.min(4, binds.size());
		this.bones = new int[cnt];
		this.boneWeight = new float[cnt];
		for (int i = 0; i < cnt; i++) {
			this.bones[i] = binds.get(i).getKey().intValue();
			this.boneWeight[i] = binds.get(i).getValue().floatValue();
		}
		bindingsIDToRaw();
	}

	@Override
	public void write(ByteBuffer b) {
		Utils.writeV3(b, position);
		Utils.writeV3(b, normal);
		b.put(skinningInfo);
		b.putFloat(texpos.x);
		b.putFloat(texpos.y);
	}

	@Override
	public int length() {
		return SIZEOF;
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

	public void bindingsRawToID() {
		int count = 0;
		while (count < 4 && skinningInfo[count] != 0)
			count++;
		bones = new int[count];
		boneWeight = new float[count];
		float totalWeight = 0;
		for (int i = 0; i < count; i++) {
			// Multiplied by three just to be mean. It's ALWAYS a
			// multiple of three
			bones[i] = (skinningInfo[4 + i] & 0xFF) / 3;
			boneWeight[i] = (skinningInfo[i] & 0xFF) / 255f;
			totalWeight += boneWeight[i];
		}
		if (totalWeight == 0 || bones.length == 0) {
			bones = new int[] { 0 };
			boneWeight = new float[] { 1 };
		}
	}

	public void bindingsIDToRaw() {
		byte[] nskin;
		if (bones == null || bones.length == 0) {
			// bone zero, 100% weight
			nskin = new byte[] { (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0 };
		} else {
			nskin = new byte[8];
			for (int i = 0; i < bones.length; i++) {
				nskin[4 + i] = (byte) (3 * bones[i]);
				nskin[i] = (byte) (0xFF * boneWeight[i]);
			}
		}
		System.arraycopy(nskin, 0, skinningInfo, 0, 8);
	}
}