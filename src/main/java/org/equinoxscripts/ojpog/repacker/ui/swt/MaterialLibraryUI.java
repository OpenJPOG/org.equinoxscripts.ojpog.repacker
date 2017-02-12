package org.equinoxscripts.ojpog.repacker.ui.swt;

import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

public class MaterialLibraryUI extends Composite {

	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public MaterialLibraryUI(Composite parent, int style) {
		super(parent, style);
		setLayout(new FormLayout());

	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
