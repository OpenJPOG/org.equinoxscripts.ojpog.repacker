package org.equinoxscripts.ojpog.repacker.pipeline;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineGroupProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineProperty;
import org.json.simple.JSONObject;

public abstract class PipelineElement {
	public final PipelineGroupProperty props;

	protected PipelineElement(PipelineProperty... props) {
		this.props = new PipelineGroupProperty("props", desc().name(), desc().desc(), props);
	}

	public PipelineElementDesc desc() {
		return getClass().getAnnotation(PipelineElementDesc.class);
	}

	public abstract TMD_File morph(TMD_File input) throws PipelineException;

	public static PipelineElement unmarshalJSON(Object e) {
		if (e == null || !(e instanceof JSONObject))
			return null;
		JSONObject o = (JSONObject) e;
		String type = (String) o.get("type");
		Object props = o.get("props");
		if (type == null)
			return null;
		try {
			Class<?> clazz = Class.forName(type);
			if (!PipelineElement.class.isAssignableFrom(clazz))
				return null;
			PipelineElement out = (PipelineElement) clazz.newInstance();
			if (props != null)
				out.props.unmarshalJSON(props);
			return out;
		} catch (Exception e1) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Object marshalJSON(PipelineElement e) {
		JSONObject o = new JSONObject();
		o.put("type", e.getClass().getName());
		o.put("props", e.props.marshalJSON());
		return o;
	}
}
