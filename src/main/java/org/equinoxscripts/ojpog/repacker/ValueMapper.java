package org.equinoxscripts.ojpog.repacker;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.equinoxscripts.ojpog.io.tmd.TMD_File;

import java.util.Set;
import java.util.TreeMap;

public class ValueMapper {
	private Map<Long, Set<String>> values = new TreeMap<>();
	private Map<Long, Set<String>> cats = new TreeMap<>();

	private Set<String> set(long code) {
		Set<String> set = values.get(code);
		if (set == null)
			values.put(code, set = new HashSet<>());
		return set;
	}

	private Set<String> cat(long code) {
		Set<String> set = cats.get(code);
		if (set == null)
			cats.put(code, set = new HashSet<>());
		return set;
	}

	public void put(long code, TMD_File s) {
		set(code).add(s.source);
		cat(code).add(s.header.category);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<Long, Set<String>> ent : values.entrySet()) {
			if (ent.getValue().isEmpty())
				continue;
			if (sb.length() > 0)
				sb.append('\n');
			sb.append(ent.getKey()).append(":\t").append(cats.get(ent.getKey())).append("\t").append(ent.getValue().toString());
		}
		return sb.toString();
	}
}
