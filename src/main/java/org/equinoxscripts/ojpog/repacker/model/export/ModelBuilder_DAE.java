package org.equinoxscripts.ojpog.repacker.model.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.io.tmd.anim.TMD_Animation;
import org.equinoxscripts.ojpog.io.tmd.anim.TMD_Channel;
import org.equinoxscripts.ojpog.io.tmd.anim.TMD_KeyFrame;
import org.equinoxscripts.ojpog.io.tmd.mesh.TMD_Mesh;
import org.equinoxscripts.ojpog.io.tmd.scene.TMD_Node;
import org.equinoxscripts.ojpog.repacker.Base;
import org.equinoxscripts.ojpog.repacker.model.export.FullMesh.FullMeshVertex;

public class ModelBuilder_DAE {
	public static void write(String id, TMD_File file) throws IOException {
		write(new File(Base.BASE_OUT, "Data/Models/" + id + ".dae"), file);
	}

	public static void write(File out, TMD_File file) throws IOException {
		new ModelBuilder_DAE(out, file, true);
	}

	private final PrintStream ww;
	private final TMD_File file;
	private final Map<TMD_Mesh, FullMesh> meshes = new HashMap<>();

	public ModelBuilder_DAE(File out, TMD_File file, boolean cleanMesh) throws FileNotFoundException {
		this.file = file;
		remapMeshes(cleanMesh);
		this.ww = new PrintStream(new BufferedOutputStream(new FileOutputStream(out)));
		ww.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		ww.println("<COLLADA xmlns=\"http://www.collada.org/2005/11/COLLADASchema\" version=\"1.4.1\">");
		writeAsset();
		writeLibraryMaterials();
		writeGeometries();
		writeControllers();
		writeAnimations();
		writeScenes();
		ww.println("</COLLADA>");
		ww.close();
	}

	private static final SimpleDateFormat FORMAT_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private void emitAnimationSource(String name, String pname, String type, String data, int count) {
		emitAnimationSource(name, new String[] { pname }, type, data, count);
	}

	private void emitAnimationSource(String name, String[] pnames, String type, String data, int count) {
		ww.println("<source id=\"" + name + "\">");
		String atype = (type.equals("Name") ? "Name" : "float") + "_array";
		ww.print("<" + atype + " id=\"" + name + "_array\" count=\"" + count + "\">");
		ww.print(data);
		ww.println("</" + atype + ">");
		ww.println("<technique_common>");
		ww.println("<accessor source=\"#" + name + "_array\" count=\"" + count + "\" stride=\""
				+ ((type.equals("float4x4") ? 16 : 1) * pnames.length) + "\">");
		for (String pname : pnames)
			if (pname != null)
				ww.println("<param name=\"" + pname + "\" type=\"" + type + "\"/>");
			else
				ww.println("<param type=\"" + type + "\"/>");
		ww.println("</accessor>");
		ww.println("</technique_common>");
		ww.println("</source>");
	}

	private void emitAnimationSampler(String name, String timeSrc, String interpSource, String dataSource) {
		ww.println("<sampler id=\"" + name + "\">");
		ww.println("<input semantic=\"INPUT\" source=\"#" + timeSrc + "\"/>");
		ww.println("<input semantic=\"OUTPUT\" source=\"#" + dataSource + "\"/>");
		ww.println("<input semantic=\"INTERPOLATION\" source=\"#" + interpSource + "\"/>");
		ww.println("</sampler>");
	}

	private void writeChannel_Samplers(TMD_Channel c) {
		String timeSrc = "src_" + c.hashCode() + "_time";
		String interpSrc = "src_" + c.hashCode() + "_interp";
		String transSrc = "src_" + c.hashCode() + "_trans";

		String transSamp = "samp_" + c.hashCode() + "_trans";
		emitAnimationSampler(transSamp, timeSrc, interpSrc, transSrc);
	}

	private void writeChannel_Links(TMD_Channel c) {
		String transSamp = "samp_" + c.hashCode() + "_trans";
		ww.println("<channel source=\"#" + transSamp + "\" target=\"" + c.nodeRef.node_name + "/trans\"/>");
	}

	private void writeChannel_Sources(TMD_Channel c) {
		StringBuilder times = new StringBuilder();
		StringBuilder trans = new StringBuilder();
		StringBuilder interpMode = new StringBuilder();
		for (TMD_KeyFrame f : c.frames) {
			if (times.length() > 0) {
				times.append(' ');
				trans.append(' ');
				interpMode.append(' ');
			}
			Matrix4f tra = new Matrix4f();
			tra.setIdentity();
			tra.setRotation(f.localRot);
			tra.setTranslation(f.localPos);
			times.append(f.time);
			trans.append(matrixToDAE(tra));
			interpMode.append("LINEAR");
		}
		String timeSrc = "src_" + c.hashCode() + "_time";
		String interpSrc = "src_" + c.hashCode() + "_interp";
		String transSrc = "src_" + c.hashCode() + "_trans";

		emitAnimationSource(timeSrc, "TIME", "float", times.toString(), c.frames.length);
		emitAnimationSource(interpSrc, "INTERPOLATION", "Name", interpMode.toString(), c.frames.length);
		emitAnimationSource(transSrc, "TRANSFORM", "float4x4", trans.toString(), c.frames.length);
	}

	private final boolean lumpNodesIntoSingleAnimation = true;

	private void writeAnimation(TMD_Animation a) {
		ww.println("<animation id=\"" + a.name + "\">");
		if (lumpNodesIntoSingleAnimation) {
			for (TMD_Channel c : a.channelNodeMap)
				if (c != null && !c.shouldIgnore())
					writeChannel_Sources(c);
			for (TMD_Channel c : a.channelNodeMap)
				if (c != null && !c.shouldIgnore())
					writeChannel_Samplers(c);
			for (TMD_Channel c : a.channelNodeMap)
				if (c != null && !c.shouldIgnore())
					writeChannel_Links(c);
		} else {
			for (TMD_Channel c : a.channelNodeMap)
				if (c != null && !c.shouldIgnore()) {
					ww.println("<animation id=\"" + a.name + "_" + c.nodeRef.node_name + "\">");
					writeChannel_Sources(c);
					writeChannel_Samplers(c);
					writeChannel_Links(c);
					ww.println("</animation>");
				}
		}
		ww.println("</animation>");
	}

	private void writeAnimations() {
		ww.println("<library_animations>");
		for (TMD_Animation a : file.animations.animations) {
			writeAnimation(a);
		}
		ww.println("</library_animations>");
	}

	private void writeControllers() {
		ww.println("<library_controllers>");
		for (FullMesh m : meshes.values()) {
			ww.println("<controller id=\"" + m.mid + "_skin\">");
			ww.println("<skin source=\"#" + m.mid + "\">");

			Map<String, Integer> nodeToID = m.requiredNodeNames();
			String[] nodeTable = new String[nodeToID.size()];
			for (Entry<String, Integer> e : nodeToID.entrySet())
				nodeTable[e.getValue().intValue()] = e.getKey();
			{
				ww.println("<source id=\"" + m.mid + "_joints\">");
				{
					ww.print("<Name_array id=\"" + m.mid + "_joints_array\" count=\"" + nodeTable.length + "\">");
					StringBuilder sb = new StringBuilder();
					for (String s : nodeTable) {
						if (sb.length() > 0)
							sb.append(' ');
						sb.append(s);
					}
					ww.print(sb.toString());
					ww.println("</Name_array>");
				}
				ww.println("<technique_common>");
				ww.println("<accessor source=\"#" + m.mid + "_joints_array\" count=\"" + nodeTable.length
						+ "\" stride=\"1\">");
				ww.println("<param name=\"JOINT\" type=\"Name\"/>");
				ww.println("</accessor>");
				ww.println("</technique_common>");
				ww.println("</source>");
			}
			{
				ww.println("<source id=\"" + m.mid + "_bind_poses\">");
				{
					ww.print("<float_array id=\"" + m.mid + "_bind_poses_array\" count=\"" + (nodeTable.length * 16)
							+ "\">");
					StringBuilder sb = new StringBuilder();
					for (String s : nodeTable) {
						if (sb.length() > 0)
							sb.append(' ');
						sb.append(matrixToDAE(file.nodes.byName(s).worldSkinningMatrix_Inv));
					}
					ww.print(sb.toString());
					ww.println("</float_array>");
				}
				ww.println("<technique_common>");
				ww.println("<accessor source=\"#" + m.mid + "_bind_poses_array\" count=\"" + nodeTable.length
						+ "\" stride=\"16\">");
				ww.println("<param name=\"TRANSFORM\" type=\"float4x4\"/>");
				ww.println("</accessor>");
				ww.println("</technique_common>");
				ww.println("</source>");
			}
			Map<Float, Integer> weights = new HashMap<>();
			{
				for (FullMeshVertex v : m.verts)
					for (Float f : v.weights.values())
						if (!weights.containsKey(f))
							weights.put(f, weights.size());
				{
					ww.println("<source id=\"" + m.mid + "_weights\">");
					{
						ww.print("<float_array id=\"" + m.mid + "_weights_array\" count=\"" + weights.size() + "\">");
						StringBuilder sb = new StringBuilder();
						float[] sp = new float[weights.size()];
						for (Entry<Float, Integer> e : weights.entrySet())
							sp[e.getValue()] = e.getKey().floatValue();
						for (float f : sp) {
							if (sb.length() > 0)
								sb.append(' ');
							sb.append(f);
						}
						ww.print(sb.toString());
						ww.println("</float_array>");
					}
					ww.println("<technique_common>");
					ww.println("<accessor source=\"#" + m.mid + "_weights_array\" count=\"" + weights.size()
							+ "\" stride=\"1\">");
					ww.println("<param name=\"WEIGHT\" type=\"float\"/>");
					ww.println("</accessor>");
					ww.println("</technique_common>");
					ww.println("</source>");
				}
			}
			{
				ww.println("<joints>");
				ww.println("<input semantic=\"JOINT\" source=\"#" + m.mid + "_joints\"/>");
				ww.println("<input semantic=\"INV_BIND_MATRIX\" source=\"#" + m.mid + "_bind_poses\"/>");
				ww.println("</joints>");
			}
			{
				ww.println("<vertex_weights count=\"" + m.verts.length + "\">");
				ww.println("<input semantic=\"JOINT\" source=\"#" + m.mid + "_joints\" offset=\"0\"/>");
				ww.println("<input semantic=\"WEIGHT\" source=\"#" + m.mid + "_weights\" offset=\"1\"/>");
				{
					ww.print("<vcount>");
					StringBuilder s = new StringBuilder(3 * m.verts.length);
					for (FullMeshVertex v : m.verts) {
						if (s.length() > 0)
							s.append(' ');
						s.append(v.weights.size());
					}
					ww.print(s.toString());
					ww.println("</vcount>");
				}
				{
					ww.print("<v>");
					StringBuilder s = new StringBuilder(8 * m.verts.length * 2);
					for (FullMeshVertex v : m.verts)
						for (Entry<String, Float> e : v.weights.entrySet()) {
							if (s.length() > 0)
								s.append(' ');
							s.append(nodeToID.get(e.getKey())).append(' ').append(weights.get(e.getValue()));
						}
					ww.print(s.toString());
					ww.println("</v>");
				}
				ww.println("</vertex_weights>");
			}
			ww.println("</skin>");
			ww.println("</controller>");
		}
		ww.println("</library_controllers>");
	}

	private void writeAsset() {
		ww.println("<asset>");
		ww.println("<created>" + FORMAT_UTC.format(new Date()) + "</created>");
		ww.println("<modified>" + FORMAT_UTC.format(new Date()) + "</modified>");
		ww.println("<up_axis>Z_UP</up_axis>");
		ww.println("</asset>");
	}

	private void remapMeshes(boolean cleanMesh) {
		for (TMD_Mesh m : file.dLoD.levels[0].members) {
			FullMesh mesh = new FullMesh(m);
			if (cleanMesh)
				mesh = mesh.clean();
			meshes.put(m, mesh);
		}
	}

	private void writeLibraryMaterials() {
		Set<String> images = new HashSet<>();
		for (TMD_Mesh m : file.dLoD.levels[0].members)
			images.add(m.material_name);
		// images
		{
			ww.println("<library_images>");
			for (String tex : images) {
				ww.println("<image id=\"" + tex + "_image\" name=\"" + tex + "_image\">");
				ww.println("<init_from>../dump/" + tex + "_0.png</init_from>");
				ww.println("</image>");
			}
			ww.println("</library_images>");
		}
		// effects
		{
			ww.println("<library_effects>");
			for (String tex : images) {
				ww.println("<effect id=\"" + tex + "_effect\">");
				ww.println("<profile_COMMON>");
				{
					ww.println("<newparam sid=\"" + tex + "_surface\">");
					ww.println("<surface type=\"2D\">");
					ww.println("<init_from>" + tex + "_image</init_from>");
					ww.println("</surface>");
					ww.println("</newparam>");
				}
				{
					ww.println("<newparam sid=\"" + tex + "_sampler\">");
					ww.println("<sampler2D>");
					ww.println("<source>" + tex + "_surface</source>");
					ww.println("</sampler2D>");
					ww.println("</newparam>");
				}
				{
					ww.println("<technique sid=\"common\"><phong>");
					for (String s : new String[] { "ambient", "diffuse" }) {
						ww.println("<" + s + ">");
						ww.println("<texture texture=\"" + tex + "_sampler\" texcoord=\"UVMap\"/>");
						ww.println("</" + s + ">");
					}
					ww.println("</phong></technique>");
				}
				ww.println("</profile_COMMON>");
				ww.println("</effect>");
			}
			ww.println("</library_effects>");
		}
		// matrials
		{
			ww.println("<library_materials>");
			for (String tex : images) {
				ww.println("<material id=\"" + tex + "_material\" name=\"" + tex + "\">");
				ww.println("<instance_effect url=\"#" + tex + "_effect\"/>");
				ww.println("</material>");
			}
			ww.println("</library_materials>");
		}
	}

	private String matrixToDAE(Matrix4f m) {
		StringBuilder sb = new StringBuilder();
		for (int r = 0; r < 4; r++)
			for (int c = 0; c < 4; c++) {
				if (c != 0 || r != 0)
					sb.append(' ');
				sb.append(m.getElement(r, c));
			}
		return sb.toString();
	}

	private void writeNode(TMD_Node n) {
		ww.println("<node id=\"" + n.node_name + "\" name=\"" + n.node_name + "\" type=\"JOINT\" sid=\"" + n.node_name
				+ "\">");
		{
			ww.println("<matrix sid=\"trans\">" + matrixToDAE(n.localPosition()) + "</matrix>");
		}
		for (TMD_Node child : n.children())
			writeNode(child);
		// {
		// ww.println("<extra>");
		// ww.println("<technique profile=\"blender\">");
		// ww.println("<layer>0</layer>");
		// ww.println("<roll>0</roll>");
		// ww.println("<connect>true</connect>");
		// ww.println("<tip_x>0</tip_x>");
		// ww.println("<tip_y>0.5</tip_y>");
		// ww.println("<tip_z>0</tip_z>");
		// ww.println("</technique>");
		// ww.println("</extra>");
		// }
		ww.println("</node>");
	}

	private void writeScenes() {
		ww.println("<library_visual_scenes>");
		ww.println("<visual_scene id=\"defaultScene\" name=\"defaultScene\">");

		TMD_Node root = null;
		for (TMD_Node n : file.nodes.nodes)
			if (n.parentRef() == null) {
				if (root == null)
					root = n;
				else
					throw new IllegalArgumentException("Multiple roots");
				writeNode(n);
			}

		for (FullMesh c : meshes.values()) {
			ww.println("<node id=\"fake_" + c.mid + "\" name=\"fake_" + c.mid + "\">");
			// ww.println("<instance_geometry url=\"#" + c.mid + "\"
			// name=\"instance_" + c.mid + "\"/>");
			ww.println("<instance_controller url=\"#" + c.mid + "_skin\">");
			ww.println("<skeleton>#" + root.node_name + "</skeleton>");
			ww.println("<bind_material>");
			ww.println("<technique_common>");
			ww.println("<instance_material symbol=\"inst_" + c.materialName + "_material\" target=\"#" + c.materialName
					+ "_material\">");
			ww.println("<bind_vertex_input semantic=\"CHANNEL1\" input_semantic=\"TEXCOORD\" input_set=\"0\"/>");
			ww.println("</instance_material>");
			ww.println("</technique_common>");
			ww.println("</bind_material>");
			ww.println("</instance_controller>");
			ww.println("</node>");
		}
		ww.println("</visual_scene>");
		ww.println("</library_visual_scenes>");
		ww.println("<scene>");
		ww.println("<instance_visual_scene url=\"#defaultScene\"/>");
		ww.println("</scene>");
	}

	private void writeGeometries() {
		ww.println("<library_geometries>");
		for (FullMesh cm : meshes.values()) {
			ww.println("<geometry id=\"" + cm.mid + "\" name=\"" + cm.mid + "\"><mesh>");
			// emit the pos, nrm, and tex
			for (String type : new String[] { "positions", "normals", "texcoords" }) {
				int stride = type.equals("texcoords") ? 2 : 3;
				ww.println("<source id=\"" + cm.mid + "_" + type + "\">");
				StringBuilder sb = new StringBuilder(5 * 3 * cm.verts.length);
				for (FullMeshVertex v : cm.verts) {
					if (sb.length() > 0)
						sb.append(' ');
					if (type.equals("texcoords"))
						sb.append(v.tex.x).append(' ').append(1 + v.tex.y);
					else {
						Vector3f p;
						if (type.equals("positions"))
							p = v.pos;
						else
							p = v.nrm;
						sb.append(p.x).append(' ').append(p.y).append(' ').append(p.z);
					}
				}
				ww.print("<float_array id=\"" + cm.mid + "_" + type + "_array\" count=\"" + (stride * cm.verts.length)
						+ "\">");
				ww.print(sb.toString());
				ww.println("</float_array>");
				{
					ww.println("<technique_common>");
					ww.println("<accessor source=\"#" + cm.mid + "_" + type + "_array\" count=\"" + cm.verts.length
							+ "\" stride=\"" + stride + "\">");
					if (type.equals("texcoords")) {
						ww.println("<param name=\"S\" type=\"float\"/>");
						ww.println("<param name=\"T\" type=\"float\"/>");
					} else {
						ww.println("<param name=\"X\" type=\"float\"/>");
						ww.println("<param name=\"Y\" type=\"float\"/>");
						ww.println("<param name=\"Z\" type=\"float\"/>");
					}
					ww.println("</accessor>");
					ww.println("</technique_common>");
				}
				ww.println("</source>");
			}
			ww.println("<vertices id=\"" + cm.mid + "_vertices\">");
			ww.println("<input semantic=\"POSITION\" source=\"#" + cm.mid + "_positions\"/>");
			ww.println("</vertices>");

			{
				int emitTris = cm.tris.length / 3;
				StringBuilder sb = new StringBuilder(3 * cm.tris.length);
				for (int i = 0; i < emitTris * 3; i++) {
					if (i > 0)
						sb.append(' ');
					sb.append(cm.tris[i] & 0xFFFF);
				}

				ww.println("<triangles count=\"" + emitTris + "\" material=\"inst_" + cm.materialName + "_material\">");
				ww.println("<input offset=\"0\" semantic=\"VERTEX\" source=\"#" + cm.mid + "_vertices\" set=\"0\"/>");
				ww.println("<input offset=\"0\" semantic=\"NORMAL\" source=\"#" + cm.mid + "_normals\" set=\"0\"/>");
				ww.println(
						"<input offset=\"0\" semantic=\"TEXCOORD\" source=\"#" + cm.mid + "_texcoords\" set=\"0\"/>");

				ww.print("<p>");
				ww.print(sb.toString());
				ww.println("</p>");
			}
			ww.println("</triangles>");
			ww.println("</mesh></geometry>");
		}
		ww.println("</library_geometries>");
	}
}
