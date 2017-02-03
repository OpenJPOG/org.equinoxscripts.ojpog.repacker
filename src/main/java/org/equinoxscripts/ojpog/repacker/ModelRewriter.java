package org.equinoxscripts.ojpog.repacker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.stream.Stream;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.model.merge.ModelMerger_DAE;

public class ModelRewriter {
	static {
		System.loadLibrary("64".equals(System.getProperty("sun.arch.data.model")) ? "gdx64" : "gdx");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models/backup").listFiles()) {
				String[] find = { "Acro.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd")
						&& (find.length == 0 || findS.map(s -> f.getName().toLowerCase().contains(s.toLowerCase()))
								.filter(s -> s).findAny().isPresent())) {
					try {
						TMD_File file = new TMD_File(f);
						File dae = new File(Base.BASE_OUT + "/Data/Models", file.source + "_mod.dae");
						if (!dae.exists())
							throw new FileNotFoundException();
						ModelMerger_DAE merge = new ModelMerger_DAE(file, ModelMerger_DAE.loadScene(dae));
						merge.apply();
						file.updateIntegrity();
						ByteBuffer output = ByteBuffer.allocate(file.length()).order(ByteOrder.LITTLE_ENDIAN);
						file.write(output);
						if (output.hasRemaining())
							System.err.println("Length wasn't equal to write");
						output.position(0);
						Utils.write(new File(base_input, "/Data/Models/" + f.getName()), output);

						TMD_File fs = new TMD_File(new File(base_input, "/Data/Models/" + f.getName()));
					} catch (Exception e) {
						System.err.println("Err " + f);
						e.printStackTrace();
						Thread.sleep(1000);
					}
					// System.out.println(TMD_Header_Block.unks);
				}
			}
		}
	}
}
