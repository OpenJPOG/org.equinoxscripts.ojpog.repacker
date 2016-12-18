package repacker.model.export;

import java.awt.Point;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// CCW
public class TriStripper {
	private final int[] rootTris;

	public TriStripper(int[] rootTris) {
		this.rootTris = rootTris;
	}

	private final Map<Point, Set<int[]>> edgeToTris = new HashMap<>();
	private final Set<int[]> tris = new TreeSet<>(new Comparator<int[]>() {
		@Override
		public int compare(int[] a, int[] b) {
			if (a[0] != b[0])
				return Integer.compare(a[0], b[0]);
			if (a[1] != b[1])
				return Integer.compare(a[1], b[1]);
			return Integer.compare(a[2], b[2]);
		}
	});

	private Set<int[]> edgeTris(Point p) {
		Set<int[]> ss = edgeToTris.get(p);
		if (ss == null)
			edgeToTris.put(p, ss = new HashSet<>());
		return ss;
	}

	private void setup() {
		tris.clear();
		edgeToTris.clear();
		strip = new int[rootTris.length * 2];
		stripHead = 0;
		statStrips = 0;
		for (int i = 0; i < rootTris.length - 2; i += 3) {
			int[] vti = new int[3];
			System.arraycopy(rootTris, i, vti, 0, 3);
			tris.add(vti);
			edgeTris(new Point(vti[0], vti[1])).add(vti);
			edgeTris(new Point(vti[1], vti[2])).add(vti);
			edgeTris(new Point(vti[2], vti[0])).add(vti);
		}
	}

	private void remove(int[] vti) {
		tris.remove(vti);
		edgeTris(new Point(vti[0], vti[1])).remove(vti);
		edgeTris(new Point(vti[1], vti[2])).remove(vti);
		edgeTris(new Point(vti[2], vti[0])).remove(vti);
	}

	private int[] strip;
	private int stripHead;
	private int statStrips;

	private void add(int v) {
		strip[stripHead++] = v;
	}

	private void makeStrip() {
		statStrips++;
		int[] tri = tris.iterator().next();
		remove(tri);

		if ((stripHead & 1) == 1) {
			int t = tri[0];
			tri[0] = tri[2];
			tri[2] = t;
		}

		if (stripHead > 0) {
			add(strip[stripHead - 1]);
			add(tri[0]);
		}
		add(tri[0]);
		add(tri[1]);
		add(tri[2]);

		if (1 > 0)
			while (true) {
				int p1 = strip[stripHead - 2];
				int p2 = strip[stripHead - 1];

				Point edge;
				if ((stripHead & 1) == 0)
					edge = new Point(p1, p2);
				else
					edge = new Point(p2, p1);

				Set<int[]> trit = edgeToTris.get(edge);
				if (trit == null || trit.isEmpty())
					return;
				int[] add = trit.iterator().next();

				if (add[0] == edge.x && add[1] == edge.y)
					add(add[2]);
				else if (add[1] == edge.x && add[2] == edge.y)
					add(add[0]);
				else if (add[2] == edge.x && add[0] == edge.y)
					add(add[1]);
				else
					throw new RuntimeException("Edge " + edge + " not in tri " + Arrays.toString(add));
				remove(add);
			}
	}

	public int[] generate() {
		setup();
		while (!tris.isEmpty()) {
			int start = stripHead;
			if (start > 0)
				start += 2;
			makeStrip();
		}
		return Arrays.copyOf(strip, stripHead);
	}
}
