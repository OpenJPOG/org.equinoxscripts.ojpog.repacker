package org.equinoxscripts.ojpog.repacker.pipeline.prop;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

public class PipelinePropertyGroup extends PipelineProperty {
	private final Map<String, PipelineProperty> props;

	public PipelinePropertyGroup(String key, String title, String tooltip, PipelineProperty... props) {
		super(key, title, tooltip);
		this.props = new HashMap<>();
		for (PipelineProperty p : props)
			this.props.put(p.key, p);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object marshalJSON() {
		JSONObject out = new JSONObject();
		for (PipelineProperty p : props.values())
			out.put(p.key, p.marshalJSON());
		return out;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void unmarshalJSON(Object json) {
		if (json != null && !(json instanceof Map)) {
			Map fs = (Map) json;
			for (PipelineProperty p : props.values())
				p.unmarshalJSON(fs.get(p.key));
		} else {
			for (PipelineProperty p : props.values())
				p.unmarshalJSON(null);
		}
	}

	public PipelineProperty property(String key) {
		return props.get(key);
	}
}
