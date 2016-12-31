package repacker.texture;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import repacker.Base;
import repacker.Utils;
import repacker.texture.TML_File.TML_Ref;

public class TextureRepacker {
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		File matsrc = new File(Base.BASE_IN[0], "Data/matlibs/dinos_flamingcliffs.tml_bak");
		File matdst = new File(Base.BASE_IN[0], "Data/matlibs/dinos_flamingcliffs.tml");
		File imgsrc = new File("C:/Users/westin/Documents/JPOG Custom Models/theri/theri_texture.png");
		String find = "Galli";

		ByteBuffer in = Utils.read(matsrc);
		TML_File tml = new TML_File(in);
		TML_Ref ref = tml.stringMapping.get(find);
		if (imgsrc.getAbsolutePath().endsWith(".dds")) {
			for (TML_Texture t : ref.refs)
				t.writeDDS(imgsrc, 1024, 1024);
		} else {
			BufferedImage img = ImageIO.read(imgsrc);
			for (TML_Texture t : ref.refs)
				t.writeImage(img, false, true);
		}

		ByteBuffer out = ByteBuffer.allocate(tml.length()).order(in.order());
		tml.write(out);
		out.flip();
		Utils.write(matdst, out);

		TML_File test = new TML_File(Utils.read(matdst));
		BufferedImage tmp = test.stringMapping.get(find).refs[0].readImage();
		ImageIO.write(tmp, "PNG", new File(imgsrc.getAbsolutePath() + "_out.png"));
	}
}
