package repacker.model.anim;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.TMD_IO;

public class TMD_Animation extends TMD_IO {
	public final String name;
	public final float length;

	// If these are both on prune the animation?
	/**
	 * This animation is strange; if this is TRUE the animation is likely
	 * broken?
	 */
	public final boolean different;
	public final boolean unk2;
	/**
	 * Number of nodes relevant to this animation?
	 */
	public final int unk3;
	public final TMD_Channel[] channelNodeMap;

	public boolean shouldPruneChannels() {
		return different && unk2;
	}

	public TMD_Animation(TMD_Animation_Block scene, ByteBuffer data) throws UnsupportedEncodingException {
		super(scene.file);
		@SuppressWarnings("unused")
		byte namelen = data.get();
		name = read(data, 15);
		different = bool(data.getInt());
		unk2 = bool(data.getInt());
		unk3 = data.getInt();

		length = data.getFloat();

		int eoa = 0;
		int offset = data.position();
		this.channelNodeMap = new TMD_Channel[file.header.numNodes];
		for (int i = 0; i < file.header.numNodes; i++) {
			int position = data.getInt(offset + 4 * i);
			data.position(file.header.rawOffsetToFile(position));
			channelNodeMap[i] = new TMD_Channel(this, data);
			channelNodeMap[i].nodeID = i;
			eoa = Math.max(eoa, data.position());
		}
		data.position(eoa);
	}

	@Override
	public int length() throws IOException {
		int len = 1 + 15;
		len += 4 + 4 + 4;
		len += 4;
		len += 4 * file.header.numNodes;
		for (TMD_Channel c : channelNodeMap)
			len += c.length();
		return len;
	}

	@Override
	public void write(ByteBuffer data) {
		data.put((byte) name.length()); // TODO doesn't always match
		write(data, 15, name);
		data.putInt(different ? 1 : 0);
		data.putInt(unk2 ? 1 : 0);
		data.putInt(unk3);

		data.putFloat(length);
		int offset = data.position() + 4 * file.header.numNodes;
		int[] pos = new int[file.header.numNodes];
		for (int i = 0; i < file.header.numNodes; i++) {
			pos[i] = offset;
			data.putInt(file.header.fileOffsetToRaw(offset));
			offset += channelNodeMap[i].length();
		}
		int ol = data.limit();
		for (int i = 0; i < file.header.numNodes; i++) {
			data.position(pos[i]);
			data.limit(i < file.header.numNodes - 1 ? pos[i + 1] : ol);
			channelNodeMap[i].write(data);
		}
		data.limit(ol);
	}

	@Override
	public void link() {
		// System.out.println(file.scene.sceneGraph(ff -> {
		// TMD_Channel a = channelNodeMap[ff.id];
		// return (a.usePositionKeys ? 1 : 0) + "" + (a.ignoreThisChannel ? 1 :
		// 0);
		// }));

		for (TMD_Channel c : channelNodeMap)
			c.link();

		// Link nodeRef:
		for (int i = 0; i < channelNodeMap.length; i++)
			channelNodeMap[i].nodeRef = file.nodes.nodes[channelNodeMap[i].nodeID];

		// Link channel data
		for (TMD_Channel c : channelNodeMap) {
			if (c.nodeRef != null) {
				for (TMD_KeyFrame f : c.frames) {
					f.localRot.set(f.rot);
					if (c.usePositionKeys)
						f.localPos.set(f.pos);
					else
						c.nodeRef.localPosition.getTranslation(f.localPos);
				}
			}
		}
	}
}
