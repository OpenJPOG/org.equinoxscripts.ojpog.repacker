package repacker.model.anim;

import java.nio.ByteBuffer;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import repacker.model.TMD_File;
import repacker.model.TMD_IO;
import repacker.model.TMD_Node;

public class TMD_Channel extends TMD_IO {
	public TMD_Node nodeRef;

	public final short unk1;
	public final TMD_KeyFrame[] frames;

	public int anim_NodeMeta;

	public TMD_Channel(TMD_File file, ByteBuffer data) {
		super(file);
		this.unk1 = data.getShort();
		this.frames = new TMD_KeyFrame[data.getShort() & 0xFFFF];
		for (int i = 0; i < this.frames.length; i++)
			this.frames[i] = new TMD_KeyFrame(file, data);
	}

	@Override
	public void link() {
		for (TMD_KeyFrame frame : frames)
			frame.link();
	}

	private int frameFor(float time) {
		if (frames[0].time > time)
			return 0;
		for (int i = 1; i < frames.length; i++)
			if (frames[i].time > time)
				return i - 1;
		return frames.length - 1;
	}

	public int value(float t, Vector3 tmp3, Quaternion tmpQ, boolean local) {
		int frame = frameFor(t);
		TMD_KeyFrame act = frames[frame];
		tmpQ.set(local ? act.localRot : act.rot);
		tmp3.set(local ? act.localPos : act.pos);
		if (frame < frames.length - 1) {
			TMD_KeyFrame ot = frames[frame + 1];
			float s = (t - act.time) / (ot.time - act.time);
			if (!Float.isFinite(s) || s < 0)
				s = 0;
			else if (s > 1)
				s = 1;
			tmpQ.slerp(local ? ot.localRot : ot.rot, s);
			tmp3.lerp(local ? ot.localPos : ot.pos, s);
		}
		return frame;
	}
}