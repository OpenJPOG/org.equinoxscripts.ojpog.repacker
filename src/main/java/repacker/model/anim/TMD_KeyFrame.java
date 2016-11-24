package repacker.model.anim;

import java.nio.ByteBuffer;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import repacker.model.TMD_IO;

public class TMD_KeyFrame extends TMD_IO {
	public final float time;
	public final int posKey, rotKey;

	public final Vector3 pos = new Vector3();
	public final Quaternion rot = new Quaternion();

	public final Vector3 localPos = new Vector3();
	public final Quaternion localRot = new Quaternion();

	public TMD_KeyFrame(TMD_Channel channel, ByteBuffer data) {
		super(channel.file);
		time = data.getFloat();
		posKey = data.getShort() & 0xFFFF;
		rotKey = data.getShort() & 0xFFFF;
	}

	@Override
	public void link() {
		if (posKey == 0 && file.tklRepo.positions.length == 0)
			this.pos.set(0, 0, 0);
		else
			this.pos.set(file.tklRepo.positions[posKey]);
		if (rotKey == 0 && file.tklRepo.rotations.length == 0)
			this.rot.idt();
		else
			this.rot.set(file.tklRepo.rotations[rotKey]);
	}

	@Override
	public String toString() {
		return "{" + time + ": " + pos.toString() + ", " + rot.toString() + "}";
	}
}