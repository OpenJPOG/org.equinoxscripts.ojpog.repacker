package repacker.model.anim;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import repacker.model.TMD_IO;
import repacker.model.TMD_Scene;

public class TMD_Animation extends TMD_IO {
	public final String name;
	public final float length;
	public final boolean unk1, unk2;
	/**
	 * Number of nodes relevant to this animation?
	 */
	public final int unk3;
	public final TMD_Channel[] channels;
	public int scene_AnimMeta;

	public TMD_Animation(TMD_Scene scene, ByteBuffer data) throws UnsupportedEncodingException {
		super(scene.file);
		byte namelen = data.get();
		name = read(data, 15).toLowerCase(); // rationalize the animation name
		unk1 = data.getInt() != 0;
		unk2 = data.getInt() != 0;
		unk3 = data.getInt();

		length = data.getFloat();

		// A table that maps channel data offsets to node IDs.
		// nodeRemap[i][0] + C == dataStart[channel with that node]
		// C is constant for each animation.
		int[][] nodeRemap = new int[scene.nodes.length][2];
		for (int i = 0; i < nodeRemap.length; i++) {
			nodeRemap[i][0] = data.getInt();
			nodeRemap[i][1] = i;
		}
		Arrays.sort(nodeRemap, (a, b) -> Integer.compare(a[0], b[0]));
		channels = new TMD_Channel[scene.nodes.length];
		for (int c = 0; c < channels.length; c++) {
			channels[c] = new TMD_Channel(this, data);
			channels[c].nodeID = nodeRemap[c][1];
		}
	}

	@Override
	public void link() {
		for (TMD_Channel c : channels)
			c.link();

		// Link nodeRef:
		for (int i = 0; i < channels.length; i++)
			channels[i].nodeRef = file.scene.nodes[channels[i].nodeID];

		if (name.contains("bld") || name.equals("walk_lp")) {
			
		}
		
		// Link channel data
		for (TMD_Channel c : channels) {
			if (c.nodeRef != null)
				for (TMD_KeyFrame f : c.frames) {
					f.localPos.set(f.pos);
					f.localRot.set(f.rot);
					// Supplied position keys seem wrong, revert to local
					// position data. TODO
					c.nodeRef.localPosition.getTranslation(f.localPos);
				}
		}
	}
}
