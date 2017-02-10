package org.equinoxscripts.ojpog.repacker.pipeline.prop;

import java.util.ArrayList;
import java.util.List;

public abstract class PipelineProperty {
	public final String key;
	public final String title;
	public final String tooltip;

	public PipelineProperty(String key, String title, String tooltip) {
		this.key = key;
		this.title = title;
		this.tooltip = tooltip;
	}

	public abstract Object marshalJSON();

	/**
	 * Read the value of this property from the given JSON object.
	 * 
	 * @param json
	 *            <em>null</em> if the value should be reset.
	 */
	public abstract void unmarshalJSON(Object json);

	private final List<Runnable> changeListeners = new ArrayList<>();

	public void addChangeListener(Runnable r) {
		changeListeners.add(r);
	}

	public boolean removeChangeListener(Runnable r) {
		return changeListeners.remove(r);
	}

	protected void dispatchChange() {
		for (Runnable r : changeListeners)
			r.run();
	}
}
