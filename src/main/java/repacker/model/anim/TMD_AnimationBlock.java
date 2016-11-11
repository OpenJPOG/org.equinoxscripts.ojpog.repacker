package repacker.model.anim;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import repacker.model.ModelExtractor;
import repacker.model.TMD_File;
import repacker.model.TMD_IO;

public class TMD_AnimationBlock extends TMD_IO {
	public final byte[] unk2 = new byte[4];
	public final TMD_Animation[] animations;

	public TMD_AnimationBlock(TMD_File file, ByteBuffer data) throws UnsupportedEncodingException {
		super(file);
		animations = new TMD_Animation[file.scene.nodes.length==8?1:4];
		for (int i = 0; i < animations.length; i++)
			animations[i] = new TMD_Animation(file, data);
		data.get(unk2);
		System.out.println(ModelExtractor.hex(unk2));
	}

	@Override
	public void link() {
		for (TMD_Animation a : animations)
			a.link();
	}
}
