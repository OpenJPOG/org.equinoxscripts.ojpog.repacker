package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import repacker.model.anim.TMD_Animation;

public class TMD_Scene extends TMD_IO {
	public final int meshCount;
	public final TMD_Node[] nodes;
	public final TMD_Animation[] animations;
	// 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 13, 14, 15, 16, 17, 18, 19, 20, 21,
	// 22, 27, 28, 31, 32, 33, 34, 35, 36, 37, 41, 44, 45, 46, 47, 48, 50, 52,
	// 57, 64, 92
	public final short unkS1;
	/**
	 * 1,2,3,4,5,6,8
	 */
	public final short animationMode;

	public TMD_Scene(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(TMD_File.SCENE_BLOCK_OFFSET);
		data.limit(file.endOfScene());

		short numNodes = data.getShort();
		unkS1 = data.getShort();
		short numAnimations = data.getShort();
		animationMode = data.getShort();
		zero(data, 44);

		int nodeHeaderOffset = file.rawOffsetToFile(data.getInt());
		int animationDataOffset, nodeArrayOffset;
		if (nodeHeaderOffset == 0x7C) {
			nodeArrayOffset = nodeHeaderOffset;
			animationDataOffset = file.rawOffsetToFile(data.getInt());
		} else {
			nodeArrayOffset = file.rawOffsetToFile(data.getInt());
			animationDataOffset = file.rawOffsetToFile(data.getInt());
		}

		if (nodeHeaderOffset != nodeArrayOffset) {
			data.position(nodeHeaderOffset);
			int[] order = new int[numNodes];
			ints(data, order);
		}

		nodes = new TMD_Node[numNodes];
		for (int i = 0; i < numNodes; i++) {
			data.position(nodeArrayOffset + i * 0xB0);
			nodes[i] = new TMD_Node(this, data, i);
		}
		for (TMD_Node n : nodes)
			n.link();

		{
			int eoa = 0;
			animations = new TMD_Animation[numAnimations];
			for (int i = 0; i < animations.length; i++) {
				int pos = file.rawOffsetToFile(data.getInt(animationDataOffset + 4 * i));
				data.position(pos);
				animations[i] = new TMD_Animation(this, data);
				eoa = Math.max(eoa, data.position());
			}
			data.position(eoa);
		}

		meshCount = data.getInt();
		if (data.hasRemaining())
			System.out.println("Didn't consume the entire scene block");
		data.limit(data.capacity());
	}

	public void write(ByteBuffer data) {
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
