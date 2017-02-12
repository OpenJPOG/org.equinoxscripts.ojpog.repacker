package org.equinoxscripts.ojpog.repacker.pipeline;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;

public class ModelPipeline {
	public final List<PipelineElement> pipeline = new ArrayList<>();

	@SuppressWarnings("unchecked")
	public Object marshalJSON() {
		JSONArray array = new JSONArray();
		for (PipelineElement e : pipeline)
			array.add(PipelineElement.marshalJSON(e));
		return array;
	}

	public void unmarshalJSON(Object o) {
		pipeline.clear();
		if (o instanceof JSONArray)
			for (Object e : (JSONArray) o) {
				PipelineElement out = PipelineElement.unmarshalJSON(e);
				if (out != null)
					pipeline.add(out);
			}
	}
}
