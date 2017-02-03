package org.equinoxscripts.ojpog.repacker.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class OptionsUI extends Composite {

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public OptionsUI(Composite parent, int style) {
		super(parent, style);
//		setLayout(new BorderLayout(0, 0));

		SashForm sashForm = new SashForm(this, SWT.BORDER);

		Label lblKey = new Label(sashForm, SWT.NONE);
		lblKey.setAlignment(SWT.CENTER);
		lblKey.setText("Key");

		Label lblValue = new Label(sashForm, SWT.NONE);
		lblValue.setAlignment(SWT.CENTER);
		lblValue.setText("Value");
		sashForm.setWeights(new int[] { 1, 1 });
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
