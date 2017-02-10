package org.equinoxscripts.ojpog.repacker.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineFileProperty;
import org.equinoxscripts.ojpog.repacker.pipeline.prop.PipelineGroupProperty;
import org.equinoxscripts.ojpog.repacker.ui.swt.pipeline.PipelinePropManager;

import swing2swt.layout.BorderLayout;

public class PipelineUI extends SashForm {

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public PipelineUI(Composite parent) {
		super(parent, SWT.VERTICAL);
		setLayout(new BorderLayout(0, 0));

		SashForm sashForm = new SashForm(this, SWT.HORIZONTAL);
		sashForm.setLayoutData(BorderLayout.CENTER);

		ScrolledComposite pipelineComposite = new ScrolledComposite(sashForm, SWT.H_SCROLL | SWT.V_SCROLL);
		pipelineComposite.setExpandHorizontal(true);
		pipelineComposite.setExpandVertical(true);

		List pipelineList = new List(pipelineComposite, SWT.BORDER);
		pipelineComposite.setContent(pipelineList);
		pipelineComposite.setMinSize(pipelineList.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		ScrolledComposite optionsComposite = new ScrolledComposite(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		optionsComposite.setExpandHorizontal(true);
		optionsComposite.setExpandVertical(true);

		PipelineGroupProperty pgp = new PipelineGroupProperty("group", "Group EN", "Group tip",
				new PipelineFileProperty("test1", "Testing 1", "Test tip 1"),
				new PipelineFileProperty("test2", "Testing 2", "Test tip 2"));

		for (Control c : optionsComposite.getChildren())
			c.dispose();
		optionsComposite.setContent(PipelinePropManager.uiFor(pgp).make(optionsComposite, SWT.NONE));

		sashForm.setWeights(new int[] { 2, 1 });

		StyledText debugText = new StyledText(this, SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP);
		debugText.setWrapIndent(4);
		debugText.setDoubleClickEnabled(false);
		debugText.setText("test1\r\ntest2\r\ntest3\r\ntest4");
		setWeights(new int[] { 221, 148 });
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
