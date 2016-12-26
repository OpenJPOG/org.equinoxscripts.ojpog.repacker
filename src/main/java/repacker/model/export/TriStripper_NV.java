package repacker.model.export;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.model.util.nvtristrip.NvFaceInfo;
import com.ardor3d.extension.model.util.nvtristrip.NvStripInfo;
import com.ardor3d.extension.model.util.nvtristrip.NvStripifier;

public class TriStripper_NV {
	private final int[] rootTris;

	public TriStripper_NV(int[] rootTris) {
		this.rootTris = rootTris;
	}

	public int[] generate() {
		List<Integer> indicies = new ArrayList<>(rootTris.length);
		int maxIndex = 0;
		for (int i : rootTris) {
			indicies.add(i);
			maxIndex = Math.max(maxIndex, i);
		}
		List<NvStripInfo> outStrips = new ArrayList<>();
		List<NvFaceInfo> outFaces = new ArrayList<>();
		NvStripifier sts = new NvStripifier();
		sts.stripify(indicies, 16, 0, maxIndex, outStrips, outFaces);

		List<Integer> outIndicies = new ArrayList<>();
		int numStrips = sts.createStrips(outStrips, outIndicies, true, false, 0);
		if (numStrips != 1)
			throw new RuntimeException();
		int[] out = new int[outIndicies.size()];
		for (int k = 0; k < outIndicies.size(); k++)
			out[k] = outIndicies.get(k).intValue();
		return out;
	}
}
