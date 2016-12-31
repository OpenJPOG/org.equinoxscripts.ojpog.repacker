package repacker.texture;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repacker.Gen_IO;

public class TML_File extends Gen_IO {
	public int unknown;
	public TML_Texture[] textures;
	public String[] stringTable;

	public class TML_Ref extends Gen_IO {
		public String name;
		public TML_Texture[] refs;
		public short unknown;

		public TML_Ref(ByteBuffer b) {
			this.name = stringTable[b.getInt()];
			this.unknown = b.getShort();
			this.refs = new TML_Texture[b.getShort()];
			for (int i = 0; i < refs.length; i++) {
				int key = b.getInt();
				for (TML_Texture t : textures)
					if (t.textureID == key) {
						this.refs[i] = t;
						break;
					}
			}
		}

		public int key() {
			int key = -1;
			for (int i = 0; i < stringTable.length; i++)
				if (name.equals(stringTable[i])) {
					key = i;
					break;
				}
			return key;
		}

		@Override
		public void write(ByteBuffer b) throws IOException {
			int key = key();
			if (key == -1)
				throw new IOException();
			b.putInt(key);
			b.putShort(unknown);
			b.putShort((short) this.refs.length);
			for (TML_Texture t : refs)
				b.putInt(t.textureID);
		}

		@Override
		public int length() throws IOException {
			return 4 + 2 + 2 + 4 * refs.length;
		}

	}

	public final Map<String, TML_Ref> stringMapping;

	public TML_File(ByteBuffer data) throws UnsupportedEncodingException, IOException {
		data.position(0);
		if (!read(data, 4).equals("TML1"))
			throw new IOException("Bad magic");
		this.unknown = data.getInt();
		this.textures = new TML_Texture[data.getInt()];
		for (int i = 0; i < this.textures.length; i++)
			this.textures[i] = new TML_Texture(data);
		stringTable = new String[data.getInt()];
		for (int i = 0; i < stringTable.length; i++)
			stringTable[i] = read(data, 32);
		this.stringMapping = new HashMap<>();
		loadInternal(data);
	}

	private void loadInternal(ByteBuffer data) {
		for (int i = 0; i < stringTable.length; i++) {
			TML_Ref ref = new TML_Ref(data);
			stringMapping.put(ref.name, ref);
		}
	}

	@Override
	public void write(ByteBuffer b) throws IOException {
		write(b, 4, "TML1");
		b.putInt(this.unknown);
		b.putInt(this.textures.length);
		for (TML_Texture t : this.textures)
			t.write(b);
		b.putInt(stringTable.length);
		for (String s : stringTable)
			write(b, 32, s);
		List<Object[]> bas = new ArrayList<>();
		for (TML_Ref r : stringMapping.values())
			bas.add(new Object[] { r.key(), r });
		bas.sort((a, bf) -> ((Integer) a[0]).compareTo((Integer) bf[0]));
		for (Object[] f : bas)
			((TML_Ref) f[1]).write(b);
	}

	@Override
	public int length() throws IOException {
		int len = 4 + 4 + 4;
		for (TML_Texture t : this.textures)
			len += t.length();
		len += 4 + 32 * stringTable.length;
		for (TML_Ref r : stringMapping.values())
			len += r.length();
		return len;
	}

}
