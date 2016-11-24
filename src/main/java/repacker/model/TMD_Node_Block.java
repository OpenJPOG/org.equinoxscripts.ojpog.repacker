package repacker.model;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public class TMD_Node_Block extends TMD_IO {
	public final TMD_Node[] nodes;

	public TMD_Node_Block(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		data.position(file.header.nodeArrayOffset);

		nodes = new TMD_Node[file.header.numNodes];
		for (int i = 0; i < file.header.numNodes; i++) {
			data.position(file.header.nodeArrayOffset + i * 0xB0);
			nodes[i] = new TMD_Node(this, data, i);
		}
	}

	@Override
	public void link() {
		for (TMD_Node node : nodes) {
			node.link();
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
