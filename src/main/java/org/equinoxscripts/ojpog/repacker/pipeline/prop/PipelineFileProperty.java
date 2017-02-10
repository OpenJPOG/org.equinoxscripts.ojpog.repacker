package org.equinoxscripts.ojpog.repacker.pipeline.prop;

import java.io.File;

public class PipelineFileProperty extends PipelineProperty {
	private File file;

	public PipelineFileProperty(String key, String title, String tooltip) {
		super(key, title, tooltip);
	}

	@Override
	public Object marshalJSON() {
		return file.getAbsolutePath();
	}

	@Override
	public void unmarshalJSON(Object json) {
		file = new File(json.toString());
		super.dispatchChange();
	}

	public File file() {
		return file;
	}
	
	public void set(File f) {
		this.file = f;
		super.dispatchChange();
	}

	public boolean accepts(File f) {
		return f.isFile();
	}
}
