package repacker.model.ext;

import java.io.File;
import java.nio.ByteBuffer;

import repacker.Base;
import repacker.Utils;

public class SCC_File {

	public static class SCC_Entry {
		public final int unk1;
		public final byte[] unk2 = new byte[8];
		public final int unk3;

		public SCC_Entry(ByteBuffer b) {
			unk1 = b.getInt();
			b.get(unk2);
			unk3 = b.getInt();
		}
	}

	public final byte[] unk1 = new byte[32];
	public final SCC_Entry[] entries;

	public SCC_File(ByteBuffer data) {
		data.get(unk1);
		entries = new SCC_Entry[data.remaining() / 32];
		System.out.println("Read " + data.remaining() + "\t" + entries.length);
		for (int i = 0; i < entries.length; i++)
			entries[i] = new SCC_Entry(data);
	}

	private static SCC_File file;

	public static SCC_File scc() {
		if (file == null) {
			byte[] buffer = new byte[1024];
			for (int i = Base.BASE_IN.length - 1; i >= 0; i--) {
				File f = new File(Base.BASE_IN[i], "Data/Models/vssver.scc");
				try {
					ByteBuffer data = Utils.read(f);
					if (data != null)
						file = new SCC_File(data);
				} catch (Exception e) {
				}
			}
		}
		return file;
	}

	public static void main(String[] args) {
		scc();
	}
}
