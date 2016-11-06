package repacker.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONObject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.data.ModelData;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMaterial;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMesh;
import com.badlogic.gdx.graphics.g3d.model.data.ModelMeshPart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNode;
import com.badlogic.gdx.graphics.g3d.model.data.ModelNodePart;
import com.badlogic.gdx.graphics.g3d.model.data.ModelTexture;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;

import repacker.Base;
import repacker.G3DModelWriter;

public class ModelExtractor {
	private static String divide(String s, int n) {
		StringBuilder out = new StringBuilder(s.length() + (s.length() / n) + 10);
		for (int i = 0; i < s.length(); i += n) {
			out.append(s.substring(i, Math.min(s.length(), i + n)));
			out.append(' ');
		}
		return out.toString();
	}

	private static String read(ByteBuffer b, int l) throws UnsupportedEncodingException {
		byte[] tmp = new byte[l];
		b.get(tmp);
		int len = 0;
		while (len < tmp.length && tmp[len++] != 0)
			;
		if (len < tmp.length || tmp[tmp.length - 1] == 0)
			len--;
		return new String(tmp, 0, len);
	}

	private static void skip(ByteBuffer b, int c) throws UnsupportedEncodingException {
		byte[] tmp = new byte[c];
		b.get(tmp);
	}

	private static void skip(ByteBuffer b, int t, int c) throws UnsupportedEncodingException {
		skip(b, t * c);
	}

	private static int[] ints(ByteBuffer b, int n) {
		int[] o = new int[n];
		for (int i = 0; i < n; i++)
			o[i] = b.getInt();
		return o;
	}

	private static short[] shorts(ByteBuffer b, int n) {
		short[] o = new short[n];
		for (int i = 0; i < n; i++)
			o[i] = b.getShort();
		return o;
	}

	private static byte[] bytes(ByteBuffer b, int n) {
		byte[] o = new byte[n];
		b.get(o);
		return o;
	}

	private static float[] floats(ByteBuffer b, int n) {
		float[] o = new float[n];
		for (int i = 0; i < n; i++)
			o[i] = b.getFloat();
		return o;
	}

	private static String hex(byte[] d) {
		StringBuilder sb = new StringBuilder();
		for (byte f : d) {
			String s = Integer.toHexString(f & 0xFF);
			if (s.length() < 2)
				sb.append("0");
			sb.append(s);
			sb.append(" ");
		}
		return sb.toString();
	}

	public static String pad(String s, int n) {
		while (s.length() < n)
			s = "0" + s;
		return s;
	}

	public static String rpad(String s, int n) {
		while (s.length() < n)
			s = s + " ";
		return s;
	}

	private static String hex(int[] d) {
		StringBuilder sb = new StringBuilder();
		for (int f : d) {
			String s = Long.toHexString(f & 0xFFFFFFFFL);
			sb.append(pad(s, 8));
			sb.append(" ");
		}
		return sb.toString();
	}

	private static class Node {
		public Node(ByteBuffer b) throws UnsupportedEncodingException {
			this.quat = new Quaternion(b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat());
			// skip identity of the transform
			skip(b, 12 * 4);
			this.translate = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			// skip last float, then inverse transform
			skip(b, 17 * 4);
			b.get();
			this.node_name = read(b, 15);
			this.parent = b.getShort();
			this.showMesh = b.getShort();
			this.pivot_offset = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
		}

		final Quaternion quat;
		final Vector3 translate;
		final String node_name;
		final short parent;
		final short showMesh;
		final Vector3 pivot_offset;

		@Override
		public String toString() {
			return node_name;
		}
	}

	private static class Vertex {
		public static final int SIZEOF = 4 * (3 + 3 + 2 + 2);

		public Vertex(ByteBuffer b) {
			position = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			normal = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			skinningInfo = new Vector2(b.getFloat(), b.getFloat());
			texpos = new Vector2(b.getFloat(), b.getFloat());
		}

		final Vector3 position, normal;
		final Vector2 skinningInfo, texpos;
	}

	private static class Mesh {
		private final Vertex[] verts;
		// this seems to be a bone mapping, NOT a nodes-with-this-mesh mapping
		private final int[] nodes;
		private final String material_name;
		private final short[] tri_strip;

		public Mesh(ByteBuffer b) throws UnsupportedEncodingException {
			byte[] unknown1 = bytes(b, 4);
			int tri_strip_size = b.getInt();
			int num_verts = b.getInt();
			material_name = read(b, 32);
			byte[] unknown2 = bytes(b, 4 + 4);
			int num_nodes = b.getInt();
			byte[] unknown3 = bytes(b, 4);

			Vector3 boundingCenter = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());
			Vector3 boundingExtents = new Vector3(b.getFloat(), b.getFloat(), b.getFloat());

			nodes = new int[num_nodes];
			for (int j = 0; j < nodes.length; j++)
				nodes[j] = b.getInt();
			verts = new Vertex[num_verts];
			for (int i = 0; i < num_verts; i++)
				verts[i] = new Vertex(b);
			tri_strip = new short[tri_strip_size];
			for (int j = 0; j < tri_strip.length; j++)
				tri_strip[j] = b.getShort();

			// System.out.println(
			// "Mesh " + material_name + " " + hex(unknown1) + " | " +
			// hex(unknown2) + " | " + hex(unknown3));
		}

		@Override
		public String toString() {
			return "MS[" + material_name + "]";
		}
	}

	// skinned, or
	public static final short MESH_TYPE_SKINNED = 8;
	// or any combination of these:
	public static final short MESH_TYPE_STATICS = 1;
	public static final short MESH_TYPE_UNKNOWN2 = 2;
	public static final short MESH_TYPE_SOME_ANIM = 4;

	// Vehicle: 1,2,5
	// Flora: 1
	// Misc: 1,2,3,4,8
	// People: 5,8
	// Struct=1,2,4,6,8
	// Others: 8

	private static final String[] SKINNED_CATS = { "dc", "dhbja", "djb", "dma", "dmbt", "df", "dha" };

	public static void main(String[] args) throws IOException, InterruptedException {
		Map<String, Set<Object>> flags = new HashMap<String, Set<Object>>();

		byte[] buffer = new byte[1024];
		int compareAgainst = -1;
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = {};// { "Brach" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd") && (find.length == 0
						|| findS.map(s -> f.getName().contains(s)).filter(s -> s).findAny().isPresent())) {
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						FileInputStream fin = new FileInputStream(f);
						while (true) {
							int s = fin.read(buffer);
							if (s > 0)
								bos.write(buffer, 0, s);
							else
								break;
						}
						fin.close();
						bos.close();
						ByteBuffer data = ByteBuffer.wrap(bos.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
						if (!read(data, 4).equals("TMDL"))
							throw new IOException("Bad magic");
						data.position(12);
						String category = read(data, 8);
						{
							boolean bad = true;
							for (String s : SKINNED_CATS)
								if (category.equalsIgnoreCase(s))
									bad = false;
							if (bad)
								continue;
						}
						// Read some header info:
						data.position(20);
						// 1 value per category except with structures
						long h_unknownCategorizing = data.getLong();
						skip(data, 4);
						int[] h_unknown3 = ints(data, 3);
						skip(data, 4, 4);

						// Read Scene Graph Block
						short numNodes = data.getShort(0x40);
						data.position(0x44);
						short unknown1 = data.getShort();
						short sceneFeatures = data.getShort();
						skip(data, 42); // zeros
						short unknown2 = data.getShort();
						short unknown3 = data.getShort();
						Node[] nodes = new Node[numNodes];
						for (int i = 0; i < numNodes; i++) {
							data.position(0x7C + i * 0xB0);
							nodes[i] = new Node(data);
						}
						data.position(0x7C + numNodes * 0xB0);
						int[] unknown4 = ints(data, 9);
						int[] nodeUnknown = ints(data, numNodes);
						// unknown4[4,5,6,7,8] -> "mostly" consistent between
						// object variants (timing or something)
						// flora/decor: ???????? 71655304 00000031 00000000
						// 00000000 00000000 00000000 ???????? 3d088889
						// besides this the information is VERY dense

						// System.out.println(sceneFeatures + "\t" + category +
						// "/" + f.getName() + " \t" + nodes.length
						// + Arrays.toString(nodes));
						int parent = -1;
						for (int i = 0; i < nodes.length; i++) {
							// node unknown appears to be a bitfield. Render
							// options?
							// high byte: render mode. (0 == normal, 1 ==
							// skinned)
							//
							// {
							// Set<Object> ffff = flags.get(category);
							// if (ffff == null)
							// flags.put(category, ffff = new
							// HashSet<Object>());
							// ffff.add(nodeUnknown[i] >> 24);
							// }
							if (nodes[i].node_name.equalsIgnoreCase("D"))
								parent = nodes[i].parent;
						}
						if (parent >= 0) {
							System.out.println(rpad(nodes[parent].node_name, 32) + "\t"
									+ divide(pad(Integer.toBinaryString(nodeUnknown[parent]), 32), 8) + "\t"
									+ pad(Integer.toHexString(nodeUnknown[parent]), 8) + "\t" + nodes[parent]);
						}
						{
							StringBuilder node_unkn = new StringBuilder();
							int s = -1;
							// if (compareAgainst == -1)
							compareAgainst = nodeUnknown[0];
							for (int i = 0; i < 33; i++) {
								int mask = (int) ((1L << i) & 0xFFFFFFFF);
								boolean constant = true;
								for (int j = 0; j < nodes.length; j++)
									constant &= (compareAgainst & mask) == (nodeUnknown[j] & mask);
								if (!constant || i == 32) {
									if (s >= 0) {
										node_unkn.append(32 - i).append("-").append((31 - s)).append("=")
												.append(divide(
														pad(Integer.toBinaryString(
																(nodeUnknown[0] >> s) & ((1 << (i - s)) - 1)), i - s),
														8))
												.append('\t');
										s = -1;
									}
								} else if (s == -1) {
									s = i;
								}
							}
							// System.out.println(node_unkn);
						}

						// Read Mesh Block
						int offs = 0x3C + data.getInt(0x1C) + 8;
						data.position(offs - 4);
						byte[] mesh_block_unk1 = bytes(data, 4);
						data.position(offs);
						int num_meshes = data.getInt();
						byte[] mesh_block_unk2 = bytes(data, 4 * 5);
						// System.out.println(hex(test1) + "\t" + hex(test));
						Mesh[] meshes = new Mesh[num_meshes];
						for (int i = 0; i < num_meshes; i++) {
							meshes[i] = new Mesh(data);
//							System.out.println(f.getName() + "\t" + i + "\t" + Arrays.stream(meshes[i].nodes)
//									.mapToObj(a -> nodes[a]).map(a -> a.node_name).collect(Collectors.toList()));
						}
						if (1 == 1)
							continue;

						String id = f.getName().substring(0, f.getName().length() - 4);
						ModelData model = new ModelData();
						model.id = id;
						model.version[0] = 0;
						model.version[1] = 1;
						ModelNode[] mns = new ModelNode[nodes.length];
						for (int i = 0; i < nodes.length; i++) {
							Node n = nodes[i];
							ModelNode mn = mns[i] = new ModelNode();
							mn.id = n.node_name;
							mn.rotation = n.quat;
							mn.translation = n.translate;
							model.nodes.add(mn);
						}
						for (int i = 0; i < nodes.length; i++) {
							Node n = nodes[i];
							if (n.parent >= 0 && n.parent < nodes.length) {
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
						for (Mesh m : meshes) {
							ModelMesh mm = new ModelMesh();
							mm.id = "m_" + m.hashCode();
							for (int n : m.nodes) {
								// if (nodes[n].showMesh != 0) {
								if (mns[n].meshId != null)
									System.err.println("Already attached mesh on " + mns[n].id);
								mns[n].meshId = mm.id;
								mns[n].parts = new ModelNodePart[1];
								mns[n].parts[0] = new ModelNodePart();
								mns[n].parts[0].meshPartId = "main";
								mns[n].parts[0].materialId = m.material_name;
								mats.add(m.material_name);
								// }
							}
							mm.attributes = new VertexAttribute[] { VertexAttribute.Position(),
									VertexAttribute.Normal(), VertexAttribute.TexCoords(0) };
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
							mm.parts[0].id = "main";
							mm.parts[0].primitiveType = GL30.GL_TRIANGLE_STRIP;
							mm.parts[0].indices = m.tri_strip;
							model.meshes.add(mm);
						}
						// System.out.println("Nodes x" + nodes.length + ":\t" +
						// Arrays.toString(nodes));

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

						File out = new File(Base.BASE_OUT,
								"Data/Models/" + category.toLowerCase() + "/" + id + ".g3dj");
						out.getParentFile().mkdirs();
						JSONObject ff = new JSONObject();
						G3DModelWriter w = new G3DModelWriter(ff);
						w.put(model);
						Writer ww = new FileWriter(out);
						ff.writeJSONString(ww);
						ww.close();
						G3dModelLoader loader = new G3dModelLoader(new JsonReader());
						ModelData output = loader.loadModelData(new FileHandle(out));
						System.out.println("Nodes:");
						for (ModelNode mn : output.nodes)
							System.out.println(category + "/" + id + "\t" + mn.id + "\t" + mn.translation);

						System.out.println("Meshes:");
						for (ModelMesh mn : output.meshes)
							System.out.println(category + "/" + id + "\t" + mn.id);
					} catch (Exception e) {
						// e.printStackTrace();
					}
				}
			}
			System.out.println(flags);
		}
	}
}
