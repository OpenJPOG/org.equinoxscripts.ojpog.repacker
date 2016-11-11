package repacker.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.JsonReader;

import repacker.Base;
import repacker.G3DModelWriter;
import repacker.model.TMD_Mesh.Vertex;
import repacker.model.anim.TMD_Animation;
import repacker.model.anim.TMD_Channel;
import repacker.model.anim.TMD_KeyFrame;

public class ModelBuilder {
	public static void write(String id, TMD_File file) throws IOException {
		ModelData model = new ModelData();
		model.id = id;
		model.version[0] = 0;
		model.version[1] = 1;
		ModelNode[] mns = new ModelNode[file.scene.nodes.length];
		Map<TMD_Node, ModelNode> modelNodes = new HashMap<>();
		for (int i = 0; i < file.scene.nodes.length; i++) {
			TMD_Node n = file.scene.nodes[i];
			ModelNode mn = mns[i] = new ModelNode();
			modelNodes.put(n, mn);
			mn.id = n.node_name;

			if (n.parent < 0 || n.parent >= file.scene.nodes.length) {
				model.nodes.add(mn);
			}
			n.localPosition.getTranslation(mn.translation = new Vector3());
			n.localPosition.getRotation(mn.rotation = new Quaternion());
		}

		for (int i = 0; i < file.scene.nodes.length; i++) {
			TMD_Node n = file.scene.nodes[i];
			if (n.parent >= 0 && n.parent < file.scene.nodes.length) {
				ModelNode[] ch = mns[n.parent].children;
				if (ch == null)
					mns[n.parent].children = new ModelNode[] { mns[i] };
				else {
					ch = mns[n.parent].children = Arrays.copyOf(ch, ch.length + 1);
					ch[ch.length - 1] = mns[i];
				}
			}
		}

		Set<String> mats = new HashSet<String>();
		for (TMD_Mesh m : file.meshes.meshes) {
			m.loadVtxAndTri();

			int[] derefVert = new int[file.scene.nodes.length];
			Arrays.fill(derefVert, -1);
			for (int i = 0; i < m.meshParentsRef.length; i++)
				derefVert[m.meshParents[i]] = i;

			ModelMesh mm = new ModelMesh();
			mm.id = "m_" + m.hashCode();
			{
				ModelNode at = null;
				ModelNode fake = null;
				if (m.isSkinned()) {
					TMD_Node basis = file.scene.nodes[0];
					fake = at = modelNodes.get(basis);
					fake = new ModelNode();
					fake.id = "mesh_alias_" + mm.id + "_" + System.nanoTime();
					{
						Matrix4 inv = new Matrix4(basis.worldPosition).inv();
						inv.getTranslation(fake.translation = new Vector3());
						inv.getRotation(fake.rotation = new Quaternion());
					}
				} else {
					for (int n : m.meshParents) {
						TMD_Node basis = file.scene.nodes[n];

						at = modelNodes.get(basis);
						fake = new ModelNode();
						fake.id = "mesh_alias_" + n + "_" + mm.id + "_" + System.nanoTime();
						{
							Matrix4 inv = new Matrix4(basis.worldPosition).inv();
							inv.getTranslation(fake.translation = new Vector3());
							inv.getRotation(fake.rotation = new Quaternion());
						}
					}
				}
				if (fake != at) {
					if (at.children == null)
						at.children = new ModelNode[1];
					else
						at.children = Arrays.copyOf(at.children, at.children.length + 1);
					at.children[at.children.length - 1] = fake;
				}

				fake.meshId = mm.id;
				fake.parts = new ModelNodePart[1];
				fake.parts[0] = new ModelNodePart();
				fake.parts[0].meshPartId = m.hashCode() + "_main";
				fake.parts[0].materialId = m.material_name;
				if (m.isSkinned()) {
					fake.parts[0].bones = new ArrayMap<>();
					for (int i = 0; i < m.meshParentsRef.length; i++) {
						TMD_Node node = m.meshParentsRef[i];
						fake.parts[0].bones.put(node.node_name, new Matrix4(node.worldPosition));
					}
				}
				mats.add(m.material_name);
			}

			int vsize;
			if (m.isSkinned()) {
				mm.attributes = new VertexAttribute[] { VertexAttribute.Position(), VertexAttribute.Normal(),
						VertexAttribute.TexCoords(0), VertexAttribute.BoneWeight(0), VertexAttribute.BoneWeight(1) };
				vsize = 3 + 3 + 2 + 4;
			} else {
				mm.attributes = new VertexAttribute[] { VertexAttribute.Position(), VertexAttribute.Normal(),
						VertexAttribute.TexCoords(0) };
				vsize = 3 + 3 + 2;
			}

			mm.vertices = new float[vsize * m.verts.length];
			for (int i = 0; i < m.verts.length; i++) {
				Vertex v = m.verts[i];
				int o = vsize * i;
				mm.vertices[o] = v.position.x;
				mm.vertices[o + 1] = v.position.y;
				mm.vertices[o + 2] = v.position.z;
				mm.vertices[o + 3] = v.normal.x;
				mm.vertices[o + 4] = v.normal.y;
				mm.vertices[o + 5] = v.normal.z;
				mm.vertices[o + 6] = v.texpos.x;
				mm.vertices[o + 7] = v.texpos.y;
				if (m.isSkinned()) {
					mm.vertices[o + 8] = derefVert[v.primaryBone];
					mm.vertices[o + 9] = v.primaryBoneAlpha;
					mm.vertices[o + 10] = derefVert[v.secondaryBone];
					mm.vertices[o + 11] = 1 - v.primaryBoneAlpha;
					if (!Float.isFinite(v.primaryBoneAlpha) || v.primaryBoneAlpha < 0 || v.primaryBoneAlpha > 1)
						System.err.println("Invalid bone alpha");
					if (mm.vertices[o + 8] < 0 || mm.vertices[o + 8] >= m.meshParents.length)
						System.err.println("Bad primary bone");
					if (mm.vertices[o + 10] < 0 || mm.vertices[o + 10] >= m.meshParents.length)
						System.err.println("Bad secondary bone");
				}
			}
			mm.parts = new ModelMeshPart[1];
			mm.parts[0] = new ModelMeshPart();
			mm.parts[0].id = m.hashCode() + "_main";
			mm.parts[0].primitiveType = GL30.GL_TRIANGLE_STRIP;
			mm.parts[0].indices = Arrays.copyOf(m.tri_strip, m.tri_strip.length);
			model.meshes.add(mm);
		}

		for (String mat : mats) {
			ModelMaterial def = new ModelMaterial();
			def.id = mat;
			ModelTexture tex = new ModelTexture();
			tex.fileName = mat + "_0.dds";
			tex.id = mat;
			tex.usage = ModelTexture.USAGE_DIFFUSE;
			def.textures = new Array<ModelTexture>();
			def.textures.add(tex);
			model.materials.add(def);
		}

		for (TMD_Animation a : file.scene.animations.animations) {
			ModelAnimation anim = new ModelAnimation();
			anim.id = a.name;
			anim.nodeAnimations = new Array<>();
			for (TMD_Channel c : a.channels) {
				if (c.nodeRef == null)
					continue;
				ModelNodeAnimation mna = new ModelNodeAnimation();
				mna.nodeId = modelNodes.get(c.nodeRef).id;
				mna.translation = new Array<>();
				mna.rotation = new Array<>();
				for (TMD_KeyFrame frame : c.frames) {
					ModelNodeKeyframe<Vector3> pos = new ModelNodeKeyframe<>();
					pos.keytime = frame.time;
					pos.value = new Vector3(frame.localPos);

					ModelNodeKeyframe<Quaternion> rot = new ModelNodeKeyframe<>();
					rot.keytime = frame.time;
					rot.value = new Quaternion(frame.localRot);

					mna.translation.add(pos);
					mna.rotation.add(rot);
				}
				anim.nodeAnimations.add(mna);
			}
			model.animations.add(anim);
		}

		File out = new File(Base.BASE_OUT, "Data/Models/" + file.category.toLowerCase() + "/" + id + ".g3dj");
		out.getParentFile().mkdirs();
		JSONObject ff = new JSONObject();
		G3DModelWriter w = new G3DModelWriter(ff);
		w.put(model);

		Writer ww = new FileWriter(out);
		ff.writeJSONString(ww);
		ww.close();
		G3dModelLoader loader = new G3dModelLoader(new JsonReader());
		ModelData output = loader.loadModelData(new FileHandle(out));
	}
}
