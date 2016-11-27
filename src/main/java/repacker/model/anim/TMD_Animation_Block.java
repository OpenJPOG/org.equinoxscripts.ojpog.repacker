package repacker.model.anim;

import java.io.IOException;
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

	@Override
	public int length() throws IOException {
		int len = 4 * animations.length;
		for (TMD_Animation a : animations)
			len += a.length();
		return len;
	}

	@Override
	public void write(ByteBuffer b) throws IOException {
		int[] pos = new int[animations.length];
		int offset = b.position() + 4 * animations.length;
		for (int i = 0; i < animations.length; i++) {
			pos[i] = offset;
			b.putInt(file.header.fileOffsetToRaw(pos[i]));
			offset += animations[i].length();
		}
		int ol = b.limit();
		for (int i = 0; i < animations.length; i++) {
			b.position(pos[i]);
			b.limit(i < file.header.numNodes - 1 ? pos[i + 1] : ol);
			animations[i].write(b);
		}
		b.limit(ol);
	}
}
