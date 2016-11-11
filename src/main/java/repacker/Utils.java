package repacker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.badlogic.gdx.math.Vector3;

public class Utils {
	public static ByteBuffer read(File f) {
		byte[] buffer = new byte[1024];
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			FileInputStream fin = new FileInputStream(f);
			while (true) {
				int s = fin.read(buffer);
				if (s > 0)
					bos.write(buffer, 0, s);
				else
					break;
			}
			fin.close();
			bos.close();
			return ByteBuffer.wrap(bos.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
		} catch (Exception e) {
			return null;
		}
	}

	public static Vector3 nearestSegmentPoint(Vector3 s0, Vector3 s1, Vector3 p) {
		Vector3 v = new Vector3(s1).sub(s0);
		Vector3 w = new Vector3(p).sub(s0);
		float c1 = v.dot(w);
		if (c1 <= 0)
			return s0;
		float c2 = v.dot(v);
		if (c2 <= c1)
			return s1;
		float b = c1 / c2;

		w.set(s0).mulAdd(v, b);
		return w;
	}
}
