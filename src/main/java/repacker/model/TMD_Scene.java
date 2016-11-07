package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Function;

public class TMD_Scene extends TMD_IO {
	public final byte[] unk1 = new byte[2];
	public final short sceneFeatures;
	public final byte[] unk2_Zero = new byte[42];
	public final byte[] unk3 = new byte[4];
	public final int[] unk4 = new int[9];
	public final TMD_Node[] nodes;

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

	public TMD_Scene(ByteBuffer data) throws UnsupportedEncodingException {
		short numNodes = data.getShort(0x40);
		data.position(0x44);
		data.get(unk1);
		sceneFeatures = data.getShort();
		data.get(unk2_Zero);
		data.get(unk3);
		nodes = new TMD_Node[numNodes];
		for (int i = 0; i < numNodes; i++) {
			data.position(0x7C + i * 0xB0);
			nodes[i] = new TMD_Node(data);
		}
		data.position(0x7C + numNodes * 0xB0);
		ints(data, unk4);
		for (int i = 0; i < numNodes; i++) {
			nodes[i].scene_Unk1 = data.getInt();

			if (nodes[i].parent >= 0) {
				TMD_Node par = nodes[nodes[i].parent];
				nodes[i].parentRef = par;
				par.childRef = Arrays.copyOf(par.childRef, par.childRef.length + 1);
				par.childRef[par.childRef.length - 1] = nodes[i];
			}
		}
	}

	private int calcMaxLen(TMD_Node node, String indent) {
		String my = indent + " - " + node.node_name;
		int l = my.length();
		for (TMD_Node n : node.childRef)
			l = Math.max(calcMaxLen(n, indent + "  "), l);
		return l;
	}

	private void graph(StringBuilder out, TMD_Node node, Function<TMD_Node, String> map, String indent, int pad) {
		StringBuilder my = new StringBuilder(pad);
		my.append(indent).append(" - ").append(node.node_name);
		while (my.length() < pad)
			my.append(' ');
		out.append(my).append(map.apply(node));
		for (TMD_Node c : node.childRef) {
			out.append("\n");
			graph(out, c, map, indent + "  ", pad);
		}
	}

	public String sceneGraph(Function<TMD_Node, String> map) {
		StringBuilder graph = new StringBuilder();
		int pad = 0;
		for (TMD_Node n : nodes)
			if (n.parentRef == null)
				pad = Math.max(pad, calcMaxLen(n, ""));
		pad += 4;

		for (TMD_Node n : nodes)
			if (n.parentRef == null) {
				graph(graph, n, map, "", pad);
				graph.append("\n");
			}
		return graph.toString();
	}
}
