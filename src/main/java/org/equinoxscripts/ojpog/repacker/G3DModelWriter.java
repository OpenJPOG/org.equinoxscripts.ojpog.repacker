package org.equinoxscripts.ojpog.repacker;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.data.ModelAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodeKeyframe;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap.Entry;

@SuppressWarnings("unchecked")
public class G3DModelWriter {
	public static final short VERSION_HI = 0;
	public static final short VERSION_LO = 1;
	protected final JSONObject output;

	public G3DModelWriter(final JSONObject output) {
		this.output = output;
	}

	public void put(ModelData model) {
		output.put("version", Arrays.asList(model.version[0], model.version[1]));
		output.put("id", model.id);

		putMeshes(model);
		putMaterials(model);
		putNodes(model);
		putAnimations(model);
	}

	private List<Short> wrap(short[] v) {
		List<Short> va = new ArrayList<Short>(v.length);
		for (short s : v)
			va.add(s);
		return va;
	}

	private List<Float> wrap(float[] v) {
		List<Float> va = new ArrayList<Float>(v.length);
		for (float s : v)
			va.add(s);
		return va;
	}

	private void putMeshes(ModelData model) {
		JSONObject[] objects = new JSONObject[model.meshes.size];
		for (int i = 0; i < model.meshes.size; i++) {
			JSONObject out = objects[i] = new JSONObject();
			ModelMesh mesh = model.meshes.get(i);
			out.put("id", mesh.id);
			out.put("attributes", putAttributes(mesh.attributes));
			out.put("vertices", wrap(mesh.vertices));

			JSONObject[] parts = new JSONObject[mesh.parts.length];
			for (int p = 0; p < parts.length; p++) {
				JSONObject pp = parts[p] = new JSONObject();
				ModelMeshPart mp = mesh.parts[p];
				pp.put("id", mp.id);
				pp.put("type", putType(mp.primitiveType));
				pp.put("indices", wrap(mp.indices));
			}
			out.put("parts", Arrays.asList(parts));
		}
		output.put("meshes", Arrays.asList(objects));
	}

	private static String unmapAttr(VertexAttribute i) {
		switch (i.usage) {
		case VertexAttributes.Usage.Position:
			return "POSITION";
		case VertexAttributes.Usage.Normal:
			return "NORMAL";
		case VertexAttributes.Usage.ColorUnpacked:
			return "COLOR";
		case VertexAttributes.Usage.ColorPacked:
			return "COLORPACKED";
		case VertexAttributes.Usage.Tangent:
			return "TANGENT";
		case VertexAttributes.Usage.BiNormal:
			return "BINORMAL";
		case VertexAttributes.Usage.TextureCoordinates:
			return "TEXCOORD";
		case VertexAttributes.Usage.BoneWeight:
			return "BLENDWEIGHT";
		default:
			throw new GdxRuntimeException("Unknown vertex attribute '" + i.usage);
		}
	}

	private List<String> putAttributes(VertexAttribute[] attributes) {
		String[] s = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++)
			s[i] = unmapAttr(attributes[i]);
		return Arrays.asList(s);
	}

	private String putType(int type) {
		if (type == GL20.GL_TRIANGLES) {
			return "TRIANGLES";
		} else if (type == GL20.GL_LINES) {
			return "LINES";
		} else if (type == GL20.GL_POINTS) {
			return "POINTS";
		} else if (type == GL20.GL_TRIANGLE_STRIP) {
			return "TRIANGLE_STRIP";
		} else if (type == GL20.GL_LINE_STRIP) {
			return "LINE_STRIP";
		} else {
			throw new GdxRuntimeException("Unknown primitive type '" + type);
		}
	}

	private void putMaterials(ModelData model) {
		JSONObject[] mtls = new JSONObject[model.materials.size];
		for (int i = 0; i < mtls.length; i++) {
			JSONObject mtl = mtls[i] = new JSONObject();
			ModelMaterial mm = model.materials.get(i);
			mtl.put("id", mm.id);
			if (mm.diffuse != null)
				mtl.put("diffuse", putColor(mm.diffuse));
			if (mm.ambient != null)
				mtl.put("ambient", putColor(mm.ambient));
			if (mm.emissive != null)
				mtl.put("emissive", putColor(mm.emissive));
			if (mm.specular != null)
				mtl.put("specular", putColor(mm.specular));
			if (mm.reflection != null)
				mtl.put("reflection", putColor(mm.reflection));

			mtl.put("shininess", mm.shininess);
			mtl.put("opacity", mm.opacity);

			if (mm.textures != null) {
				JSONObject[] textures = new JSONObject[mm.textures.size];
				for (int t = 0; t < textures.length; t++) {
					JSONObject tex = textures[t] = new JSONObject();
					ModelTexture mt = mm.textures.get(t);
					tex.put("id", mt.id);
					tex.put("filename", mt.fileName);
					if (mt.uvTranslation != null)
						tex.put("uvTranslation", putVector2(mt.uvTranslation));
					if (mt.uvScaling != null)
						tex.put("uvScaling", putVector2(mt.uvScaling));
					tex.put("type", putTextureUsage(mt.usage));
				}
				mtl.put("textures", Arrays.asList(textures));
			}
		}
		output.put("materials", Arrays.asList(mtls));
	}

	private String putTextureUsage(int tex) {
		switch (tex) {
		case ModelTexture.USAGE_AMBIENT:
			return "AMBIENT";
		case ModelTexture.USAGE_BUMP:
			return "BUMP";
		case ModelTexture.USAGE_DIFFUSE:
			return "DIFFUSE";
		case ModelTexture.USAGE_EMISSIVE:
			return "EMISSIVE";
		case ModelTexture.USAGE_NONE:
			return "NONE";
		case ModelTexture.USAGE_NORMAL:
			return "NORMAL";
		case ModelTexture.USAGE_REFLECTION:
			return "REFLECTION";
		case ModelTexture.USAGE_SHININESS:
			return "SHININESS";
		case ModelTexture.USAGE_SPECULAR:
			return "SPECULAR";
		case ModelTexture.USAGE_TRANSPARENCY:
			return "TRANSPARENCY";
		default:
			return "UNKNOWN";
		}
	}

	public List<Float> putColor(Color c) {
		return Arrays.asList(c.r, c.g, c.b);
	}

	public List<Float> putVector2(Vector2 v) {
		return Arrays.asList(v.x, v.y);
	}

	private void putNodes(ModelData model) {
		List<JSONObject> obj = new ArrayList<JSONObject>();
		for (ModelNode m : model.nodes) {
			obj.add(putNodeRecurse(m));
		}
		output.put("nodes", obj);
	}

	private JSONObject putNodeRecurse(ModelNode node) {
		JSONObject out = new JSONObject();
		out.put("id", node.id);
		if (node.translation != null && !node.translation.isZero())
			out.put("translation", putVector3(node.translation));
		if (node.rotation != null && !node.rotation.isIdentity())
			out.put("rotation", putQuaternion(node.rotation));
		if (node.scale != null && (node.scale.x != 1 || node.scale.y != 1 || node.scale.z != 1))
			out.put("scale", putVector3(node.scale));
		if (node.meshId != null)
			out.put("mesh", node.meshId);

		if (node.parts != null) {
			JSONObject[] oa = new JSONObject[node.parts.length];
			for (int p = 0; p < oa.length; p++) {
				JSONObject o = oa[p] = new JSONObject();
				ModelNodePart pa = node.parts[p];
				o.put("meshpartid", pa.meshPartId);
				o.put("materialid", pa.materialId);
				if (pa.bones != null) {
					List<JSONObject> bones = new ArrayList<JSONObject>();
					for (Entry<String, Matrix4> bone : pa.bones.entries()) {
						JSONObject b = new JSONObject();
						b.put("node", bone.key);
						Vector3 scale = new Vector3(), translate = new Vector3();
						Quaternion rot = new Quaternion();
						bone.value.getTranslation(translate);
						bone.value.getRotation(rot);
						bone.value.getScale(scale);
						if (!translate.isZero())
							b.put("translation", putVector3(translate));
						if (!rot.isIdentity())
							b.put("rotation", putQuaternion(rot));
						if (scale.x != 1 || scale.y != 1 || scale.z != 1)
							b.put("scale", putVector3(scale));
						bones.add(b);
					}
					o.put("bones", bones);
				}
			}
			out.put("parts", Arrays.asList(oa));
		}

		if (node.children != null) {
			JSONObject[] o = new JSONObject[node.children.length];
			for (int i = 0; i < node.children.length; i++)
				o[i] = putNodeRecurse(node.children[i]);
			out.put("children", Arrays.asList(o));
		}
		return out;
	}

	private List<Float> putVector3(Vector3 v) {
		return Arrays.asList(v.x, v.y, v.z);
	}

	private List<Float> putQuaternion(Quaternion q) {
		return Arrays.asList(q.x, q.y, q.z, q.w);
	}

	private void putAnimations(ModelData model) {
		if (model.animations == null || model.animations.size == 0)
			return;
		JSONObject[] anim = new JSONObject[model.animations.size];
		for (int i = 0; i < anim.length; i++) {
			JSONObject ao = anim[i] = new JSONObject();
			ModelAnimation ai = model.animations.get(i);
			ao.put("id", ai.id);
			JSONObject[] na = new JSONObject[ai.nodeAnimations.size];
			for (int j = 0; j < na.length; j++) {
				JSONObject nao = na[j] = new JSONObject();
				ModelNodeAnimation nai = ai.nodeAnimations.get(j);
				nao.put("boneId", nai.nodeId);

				if (nai.translation != null) {
					JSONObject[] v = new JSONObject[nai.translation.size];
					for (int k = 0; k < nai.translation.size; k++) {
						v[k] = new JSONObject();
						ModelNodeKeyframe<Vector3> frame = nai.translation.get(k);
						v[k].put("keytime", frame.keytime);
						if (frame.value != null)
							v[k].put("value", putVector3(frame.value));
					}
					nao.put("translation", Arrays.asList(v));
				}

				if (nai.rotation != null) {
					JSONObject[] v = new JSONObject[nai.rotation.size];
					for (int k = 0; k < nai.rotation.size; k++) {
						v[k] = new JSONObject();
						ModelNodeKeyframe<Quaternion> frame = nai.rotation.get(k);
						v[k].put("keytime", frame.keytime);
						if (frame.value != null)
							v[k].put("value", putQuaternion(frame.value));
					}
					nao.put("rotation", Arrays.asList(v));
				}

				if (nai.scaling != null) {
					JSONObject[] v = new JSONObject[nai.scaling.size];
					for (int k = 0; k < nai.scaling.size; k++) {
						v[k] = new JSONObject();
						ModelNodeKeyframe<Vector3> frame = nai.scaling.get(k);
						v[k].put("keytime", frame.keytime);
						if (frame.value != null)
							v[k].put("value", putVector3(frame.value));
					}
					nao.put("scaling", Arrays.asList(v));
				}
			}
			ao.put("bones", Arrays.asList(na));
		}
		output.put("animations", Arrays.asList(anim));
	}
}