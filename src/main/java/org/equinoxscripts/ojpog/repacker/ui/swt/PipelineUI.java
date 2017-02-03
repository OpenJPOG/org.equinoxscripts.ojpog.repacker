package org.equinoxscripts.ojpog.repacker.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.custom.CBanner;
import org.eclipse.swt.layout.FillLayout;

public class PipelineUI extends SashForm {

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public PipelineUI(Composite parent, int style) {
		super(parent, style);
		
		CBanner banner = new CBanner(this, SWT.NONE);
		
		ScrolledComposite pipelineComposite = new ScrolledComposite(banner, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		banner.setLeft(pipelineComposite);
		pipelineComposite.setExpandHorizontal(true);
		pipelineComposite.setExpandVertical(true);
		
		List pipelineList = new List(pipelineComposite, SWT.BORDER);
		pipelineComposite.setContent(pipelineList);
		pipelineComposite.setMinSize(pipelineList.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		ScrolledComposite optionsComposite = new ScrolledComposite(banner, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		banner.setRight(optionsComposite);
		optionsComposite.setExpandHorizontal(true);
		optionsComposite.setExpandVertical(true);
		
		ScrolledComposite debugComposite = new ScrolledComposite(banner, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		banner.setBottom(debugComposite);
		debugComposite.setExpandHorizontal(true);
		debugComposite.setExpandVertical(true);
		setWeights(new int[] {1});
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
