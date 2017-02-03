package org.equinoxscripts.ojpog.repacker;

import java.util.Arrays;

public class BinaryConstants {
	private int refValue;
	private int[] constantBoundaries;

	public void submit(int v) {
		if (constantBoundaries == null) {
			refValue = v;
			constantBoundaries = new int[] { 0, 32 };
		} else {
			int k = 0;
			int[] test = new int[64];
			for (int i = 0; i < constantBoundaries.length; i += 2) {
				int left = constantBoundaries[i];
				int right = constantBoundaries[i + 1];

				int cstStart = -1;
				for (int j = left; j < right; j++) {
					long mask = 1 << j;
					if ((refValue & mask) != (v & mask)) {
						if (cstStart != -1) {
							test[k++] = cstStart;
							test[k++] = j;
						}
						cstStart = -1;
					} else if (cstStart == -1) {
						cstStart = j;
					}
				}
				if (cstStart != -1) {
					test[k++] = cstStart;
					test[k++] = right;
				}
			}
			this.constantBoundaries = Arrays.copyOf(test, k);
		}
	}

	public int[] boundaries() {
		if (constantBoundaries == null || constantBoundaries.length <= 0)
			return new int[0];
		return Arrays.copyOf(constantBoundaries, constantBoundaries.length);
	}

	public int[] highBoundary() {
		if (constantBoundaries == null || constantBoundaries.length <= 0)
			return new int[2];
		int[] out = new int[2];
		System.arraycopy(constantBoundaries, constantBoundaries.length - 2, out, 0, 2);
		return out;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < constantBoundaries.length; i += 2) {
			int left = constantBoundaries[i];
			int right = constantBoundaries[i + 1];
			long mask = (1L << (right - left)) - 1;
			s.append(left).append("-").append(right).append("=")
					.append(ModelExtractor.pad(Long.toBinaryString((refValue >> left) & mask), right - left))
					.append(", ");
		}
		return s.toString();
	}
}
