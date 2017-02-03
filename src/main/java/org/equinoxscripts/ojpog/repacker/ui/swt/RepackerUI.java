package org.equinoxscripts.ojpog.repacker.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class RepackerUI {

	protected Shell shell;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			RepackerUI window = new RepackerUI();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(1280, 720);
		shell.setText("SWT Application");
		shell.setLayout(new FillLayout(SWT.HORIZONTAL));

		TabFolder tabs = new TabFolder(shell, SWT.NONE);

		TabItem tbtmPipeline = new TabItem(tabs, SWT.NONE);
		tbtmPipeline.setText("Pipeline");

		PipelineUI pipelineEditor = new PipelineUI(tabs, SWT.NONE);
		tbtmPipeline.setControl(pipelineEditor);
		
//		TabItem tbtmMaterialLibrary = new TabItem(tabs, SWT.NONE);
//		tbtmMaterialLibrary.setText("Material Library");
//
//		MaterialLibraryUI materialLibraryEditor = new MaterialLibraryUI(tabs, SWT.NONE);
//		tbtmMaterialLibrary.setControl(materialLibraryEditor);

		TabItem tbtmOptions = new TabItem(tabs, SWT.NONE);
		tbtmOptions.setText("Options");

		OptionsUI optionsEditor = new OptionsUI(tabs, SWT.NONE);
		tbtmOptions.setControl(optionsEditor);

	}
}
