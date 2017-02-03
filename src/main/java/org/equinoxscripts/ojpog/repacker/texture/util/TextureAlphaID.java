package org.equinoxscripts.ojpog.repacker.texture.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.imageio.ImageIO;

public class TextureAlphaID {
	public enum AlphaType {
		NONE, BINARY, FLOAT;
	}

	public static AlphaType typeOf(File f) throws IOException {
		BufferedImage img = ImageIO.read(f);
		int[] px = new int[img.getWidth() * img.getHeight()];
		img.getRGB(0, 0, img.getWidth(), img.getHeight(), px, 0, img.getWidth());

		boolean hasAlpha = false;
		int[] bins = new int[5];
		for (int rgb : px) {
			int alpha = (rgb >> 24) & 0xFF;
			hasAlpha |= (alpha < 0xE0);
			int bin = alpha * bins.length / 256;
			bins[bin]++;
		}
		int ctr = 0;
		for (int i = 1; i < bins.length - 1; i++)
			ctr += bins[i];

		if (ctr * 2 >= bins[bins.length - 1])
			return AlphaType.FLOAT;
		else if (hasAlpha)
			return AlphaType.BINARY;
		else
			return AlphaType.NONE;
	}

	public static void main(String[] args) throws IOException {
		File root = new File("..//data/output/Data/dump/");
		PrintStream out = new PrintStream(new FileOutputStream(new File(root, "alpha.txt")));
		for (File tex : root.listFiles()) {
			if (tex.getName().endsWith(".png")) {
				String pure = tex.getName().substring(0, tex.getName().lastIndexOf('.'));
				out.println(pure + "=" + typeOf(tex));
			}
		}
		out.close();
	}
}
