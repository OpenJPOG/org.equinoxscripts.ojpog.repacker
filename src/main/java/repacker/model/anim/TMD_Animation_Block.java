package repacker.model.anim;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_Animation_Block extends TMD_IO {

	public final TMD_Animation[] animations;

	public TMD_Animation_Block(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);

		animations = new TMD_Animation[file.header.numAnimations];
		for (int i = 0; i < animations.length; i++) {
			data.position(file.header.rawOffsetToFile(data.getInt(file.header.animationDataOffset + 4 * i)));
			animations[i] = new TMD_Animation(this, data);
		}
	}

	@Override
	public void link() {
		for (TMD_Animation a : animations)
			a.link();
	}
}
