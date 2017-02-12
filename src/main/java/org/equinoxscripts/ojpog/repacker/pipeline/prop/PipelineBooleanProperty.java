package org.equinoxscripts.ojpog.repacker.pipeline.prop;

public class PipelineBooleanProperty extends PipelineProperty {
	private boolean value;

	public final boolean defaultValue;

	public PipelineBooleanProperty(String key, String title, String tooltip, boolean defaultValue) {
		super(key, title, tooltip);
		this.defaultValue = defaultValue;
	}

	@Override
	public Object marshalJSON() {
		return Boolean.valueOf(value);
	}

	@Override
	public void unmarshalJSON(Object json) {
		if (json != null && json instanceof Boolean)
			value = ((Boolean) json).booleanValue();
		else
			value = defaultValue;
		super.dispatchChange();
	}

	public void set(boolean b) {
		this.value = b;
		super.dispatchChange();
	}

	public boolean get() {
		return this.value;
	}
}
