package org.equinoxscripts.ojpog.repacker.pipeline.prop;

import java.io.File;

public class PipelineFileProperty extends PipelineProperty {
	private File file;

	public boolean mustExist;
	public boolean acceptsDirectory;
	public String[] acceptedExtensions;

	public PipelineFileProperty(String key, String title, String tooltip) {
		super(key, title, tooltip);
		this.mustExist = false;
		this.acceptsDirectory = false;
		this.acceptedExtensions = null;
	}

	public PipelineFileProperty filter(boolean mustExist, boolean directory, String... extensions) {
		this.mustExist = mustExist;
		this.acceptsDirectory = directory;
		if (this.acceptsDirectory)
			this.acceptedExtensions = null;
		else
			this.acceptedExtensions = extensions;
		return this;
	}

	@Override
	public Object marshalJSON() {
		return file != null ? file.getAbsolutePath() : "null";
	}

	@Override
	public void unmarshalJSON(Object json) {
		if (json == null || json.toString().equals("null"))
			file = null;
		else
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
		return acceptDebug(f) == null;
	}

	public String acceptDebug(File f) {
		if (!f.exists() && mustExist)
			return "File doesn't exist";
		if (f.isFile() && this.acceptsDirectory)
			return "Only accepts directories; this is a file";
		if (f.isDirectory() && !this.acceptsDirectory)
			return "Only accepts files; this is a directory";
		if (!this.acceptsDirectory & this.acceptedExtensions != null && this.acceptedExtensions.length > 0) {
			String path = f.getName();
			int dot = path.lastIndexOf('.');
			String ext = dot > 0 ? path.substring(dot + 1) : "";

			for (String s : this.acceptedExtensions)
				if (s.equalsIgnoreCase(ext))
					return null;
			StringBuilder supported = new StringBuilder();
			for (String s : this.acceptedExtensions) {
				if (supported.length() > 0)
					supported.append(", ");
				supported.append(s);
			}
			return "extension " + ext + " isn't supported; must be one of: " + supported.toString();
		}
		return null;
	}
}
