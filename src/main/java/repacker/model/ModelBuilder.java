package repacker.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial.MaterialType;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;

import repacker.Base;
import repacker.G3DModelWriter;
import repacker.model.TMD_Mesh.Vertex;

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
			Matrix4 me = n.worldPosition;

			if (n.parent < 0 || n.parent >= file.scene.nodes.length) {
				model.nodes.add(mn);
			} else {
				TMD_Node p = n.parentRef;
				Matrix4 inv = new Matrix4(p.worldPosition).inv();
				me = inv.mul(me);
			}
			me.getTranslation(mn.translation = new Vector3());
			me.getRotation(mn.rotation = new Quaternion());
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

			ModelMesh mm = new ModelMesh();
			mm.id = "m_" + m.hashCode();
			for (int n : m.meshParents) {
				TMD_Node basis = file.scene.nodes[n];

				ModelNode at = modelNodes.get(basis);
				ModelNode fake = new ModelNode();
				{
					if (at.children == null)
						at.children = new ModelNode[1];
					else
						at.children = Arrays.copyOf(at.children, at.children.length + 1);
					at.children[at.children.length - 1] = fake;
				}

				fake.id = "mesh_alias_" + n + "_" + mm.id + "_" + System.nanoTime();
				{
					Matrix4 inv = new Matrix4(basis.worldPosition).inv();
					inv.getTranslation(fake.translation = new Vector3());
					inv.getRotation(fake.rotation = new Quaternion());
				}
				fake.meshId = mm.id;
				fake.parts = new ModelNodePart[1];
				fake.parts[0] = new ModelNodePart();
				fake.parts[0].meshPartId = m.hashCode() + "_main";
				fake.parts[0].materialId = m.material_name;
				mats.add(m.material_name);
			}

			mm.attributes = new VertexAttribute[] { VertexAttribute.Position(), VertexAttribute.Normal(),
					VertexAttribute.TexCoords(0) };
			int vsize = 3 + 3 + 2;
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

		// {
		// PrintStream wr = new PrintStream(new
		// FileOutputStream("C:/Users/westin/Downloads/test.stl"));
		// wr.println("solid test");
		// for (ModelNode node : output.nodes)
		// dumpSTL(wr, output, node, new Vector3());
		// wr.println("endsolid test");
		// wr.close();
		// }
		//
		// int i = 0;
		// for (ModelMesh mm : output.meshes) {
		// PrintStream wr = new PrintStream(new
		// FileOutputStream("C:/Users/westin/Downloads/test_mesh_" + i +
		// ".stl"));
		// wr.println("solid test");
		// dumpSTL(wr, mm, new Vector3());
		// wr.println("endsolid test");
		// wr.close();
		// i++;
		// }
	}

	private static void dumpSTL(PrintStream wr, ModelMesh mesh, Vector3 base) {
		for (ModelMeshPart mmp : mesh.parts) {
			for (int i = 2; i < mmp.indices.length; i++) {
				int ao = mmp.indices[i - 2] * (3 + 3 + 2);
				int bo = mmp.indices[i - 1] * (3 + 3 + 2);
				int co = mmp.indices[i - 0] * (3 + 3 + 2);

				Vector3 a = new Vector3(mesh.vertices[ao], mesh.vertices[ao + 1], mesh.vertices[ao + 2]);
				a = new Vector3(a).add(base);
				Vector3 b = new Vector3(mesh.vertices[bo], mesh.vertices[bo + 1], mesh.vertices[bo + 2]);
				b = new Vector3(b).add(base);
				Vector3 c = new Vector3(mesh.vertices[co], mesh.vertices[co + 1], mesh.vertices[co + 2]);
				c = new Vector3(c).add(base);
				wr.println("facet normal 1 0 0");
				wr.println("outer loop");
				wr.println("vertex " + a.x + " " + a.y + " " + a.z);
				wr.println("vertex " + b.x + " " + b.y + " " + b.z);
				wr.println("vertex " + c.x + " " + c.y + " " + c.z);
				wr.println("endloop");
				wr.println("endfacet");
			}
		}
	}

	private static void dumpSTL(PrintStream wr, ModelData model, ModelNode node, Vector3 base) {
		base = new Vector3(base);
		if (node.translation != null)
			base.add(node.translation);
		if (node.meshId != null) {
			ModelMesh mesh = null;
			for (ModelMesh m : model.meshes)
				if (m.id.equals(node.meshId))
					mesh = m;
			for (ModelNodePart part : node.parts) {
				ModelMeshPart mmp = null;
				for (ModelMeshPart p : mesh.parts)
					if (p.id.equals(part.meshPartId))
						mmp = p;

				for (int i = 2; i < mmp.indices.length; i++) {
					int ao = mmp.indices[i - 2] * (3 + 3 + 2);
					int bo = mmp.indices[i - 1] * (3 + 3 + 2);
					int co = mmp.indices[i - 0] * (3 + 3 + 2);

					Vector3 a = new Vector3(mesh.vertices[ao], mesh.vertices[ao + 1], mesh.vertices[ao + 2]);
					a = new Vector3(a).add(base);
					Vector3 b = new Vector3(mesh.vertices[bo], mesh.vertices[bo + 1], mesh.vertices[bo + 2]);
					b = new Vector3(b).add(base);
					Vector3 c = new Vector3(mesh.vertices[co], mesh.vertices[co + 1], mesh.vertices[co + 2]);
					c = new Vector3(c).add(base);
					wr.println("facet normal 1 0 0");
					wr.println("outer loop");
					wr.println("vertex " + a.x + " " + a.y + " " + a.z);
					wr.println("vertex " + b.x + " " + b.y + " " + b.z);
					wr.println("vertex " + c.x + " " + c.y + " " + c.z);
					wr.println("endloop");
					wr.println("endfacet");
				}
			}
		}

		if (node.children != null)
			for (ModelNode c : node.children)
				dumpSTL(wr, model, c, base);
	}
}
