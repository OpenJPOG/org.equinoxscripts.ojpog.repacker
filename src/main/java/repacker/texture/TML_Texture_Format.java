package repacker.texture;

public enum TML_Texture_Format {
	RGBA_8888(0), RGBA_5551(2), RAW_DDS(6), RGBA_4444(7);

	public final short id;

	private TML_Texture_Format(int id) {
		this.id = (short) id;
	}

	private static final TML_Texture_Format[] formats;
	static {
		int le = 0;
		for (TML_Texture_Format f : values())
			le = Math.max(le, f.id + 1);
		formats = new TML_Texture_Format[le];
		for (TML_Texture_Format f : values())
			formats[f.id] = f;
	}

	public static TML_Texture_Format unpack(short s) {
		return formats[s];
	}
}
