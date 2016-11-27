package repacker;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.stream.Stream;

import repacker.model.TMD_File;

public class ModelRewriter {
	static {
		System.loadLibrary("64".equals(System.getProperty("sun.arch.data.model")) ? "gdx64" : "gdx");
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		for (File base_input : Base.BASE_IN) {
			for (File f : new File(base_input, "Data/Models").listFiles()) {
				String[] find = { "Cerato.tmd","Cerato_hi.tmd","Cerato_lo.tmd","Cerato_md.tmd" };
				Stream<String> findS = Arrays.stream(find);
				if (f.getName().endsWith(".tmd")
						&& (find.length == 0 || findS.map(s -> f.getName().toLowerCase().contains(s.toLowerCase()))
								.filter(s -> s).findAny().isPresent())) {
					try {
						ByteBuffer data = Utils.read(f);
						TMD_File file = new TMD_File(f.getName().substring(0, f.getName().length() - 4), data);
						ByteBuffer output = ByteBuffer.allocate(file.length()).order(ByteOrder.LITTLE_ENDIAN);
						file.write(output);
						output.position(0);
						TMD_File exp = new TMD_File("TEST", output);
						output.position(0);
						Utils.write(new File(Base.BASE_OUT + "/Data/Models", f.getName()), output);
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
