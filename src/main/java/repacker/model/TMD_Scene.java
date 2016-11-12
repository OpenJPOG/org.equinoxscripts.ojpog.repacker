package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.function.Function;

import repacker.model.anim.TMD_Animation;

public class TMD_Scene extends TMD_IO {
	public final byte[] unk2_Zero = new byte[42];
	public final byte[] unk3 = new byte[4];
	public final int[] unk4 = new int[9];
	public final int[] unk5 = new int[4];
	public final int unk6;
	public final TMD_Node[] nodes;
	public final TMD_Animation[] animations;
	public final short unkS1, unkS3;

	public TMD_Scene(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(0x40);
		data.limit(0x40 + data.getInt(0x1C));
		short numNodes = data.getShort();
		unkS1 = data.getShort();
		short numAnimations = data.getShort();
		unkS3 = data.getShort();
		data.get(unk2_Zero);
		data.get(unk3);
		nodes = new TMD_Node[numNodes];
		for (int i = 0; i < numNodes; i++) {
			data.position(0x7C + i * 0xB0);
			nodes[i] = new TMD_Node(this, data, i);
		}
		for (TMD_Node n : nodes)
			n.link();
		data.position(0x7C + numNodes * 0xB0);
		int[] animationMeta = new int[numAnimations];
		ints(data, animationMeta);
		animations = new TMD_Animation[numAnimations];
		for (int i = 0; i < animations.length; i++) {
			animations[i] = new TMD_Animation(this, data);
			animations[i].scene_AnimMeta = animationMeta[i];
		}
		unk6 = data.getInt();
		data.limit(data.capacity());
	}

	@Override
	public void link() {
		// Nodes are linked in constructor.
		for (TMD_Animation a : animations)
			a.link();
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
