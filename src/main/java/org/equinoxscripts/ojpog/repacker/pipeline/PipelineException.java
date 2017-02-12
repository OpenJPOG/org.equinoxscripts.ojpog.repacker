package org.equinoxscripts.ojpog.repacker.pipeline;

public class PipelineException extends Exception {
	private static final long serialVersionUID = 1L;

	public final String name, details;

	public PipelineException(String name, String details, Throwable e) {
		super(name, e);

		this.name = name;
		this.details = details;
	}

	public PipelineException(String name, String details) {
		super(name);

		this.name = name;
		this.details = details;
	}
}
