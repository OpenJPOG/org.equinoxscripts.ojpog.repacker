package repacker.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import jassimp.AiScene;
import repacker.Utils;
import repacker.model.TMD_File;
import repacker.model.merge.ModelMerger_DAE;

public class ModelMerger extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextField txtOutputFile;
	private JTextField txtInputFile;
	private JTextField txtColladaFile;
	private final JLabel lblStats;

	private File sourceFile, colladaFile, destFile;
	private TMD_File model;
	private AiScene colladaIn;
	private final JButton btnSave;

	public ModelMerger() {
		setLayout(new BorderLayout(0, 0));
		JPanel entireTop = new JPanel();
		entireTop.setLayout(new GridLayout(0, 1));
		add(entireTop, BorderLayout.NORTH);
		{
			JPanel panelInput = new JPanel();
			entireTop.add(panelInput);
			GridBagLayout gbl_panelInput = new GridBagLayout();
			gbl_panelInput.columnWidths = new int[] { 45, 405, 0, 0 };
			gbl_panelInput.rowHeights = new int[] { 23, 20, 0 };
			gbl_panelInput.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
			gbl_panelInput.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			panelInput.setLayout(gbl_panelInput);

			JLabel lblInputFile = new JLabel("Input File");
			GridBagConstraints gbc_lblInputFile = new GridBagConstraints();
			gbc_lblInputFile.weighty = 1.0;
			gbc_lblInputFile.fill = GridBagConstraints.BOTH;
			gbc_lblInputFile.insets = new Insets(0, 0, 5, 5);
			gbc_lblInputFile.gridx = 0;
			gbc_lblInputFile.gridy = 0;
			panelInput.add(lblInputFile, gbc_lblInputFile);

			txtInputFile = new JTextField();
			GridBagConstraints gbc_txtInputFile = new GridBagConstraints();
			gbc_txtInputFile.insets = new Insets(0, 0, 5, 5);
			gbc_txtInputFile.weighty = 1.0;
			gbc_txtInputFile.weightx = 1.0;
			gbc_txtInputFile.fill = GridBagConstraints.BOTH;
			gbc_txtInputFile.gridx = 1;
			gbc_txtInputFile.gridy = 0;
			panelInput.add(txtInputFile, gbc_txtInputFile);
			txtInputFile.setColumns(10);

			JButton btnBrowseInput = new JButton("...");
			GridBagConstraints gbc_btnBrowseInput = new GridBagConstraints();
			gbc_btnBrowseInput.weighty = 1.0;
			gbc_btnBrowseInput.fill = GridBagConstraints.BOTH;
			gbc_btnBrowseInput.insets = new Insets(0, 0, 5, 0);
			gbc_btnBrowseInput.gridx = 2;
			gbc_btnBrowseInput.gridy = 0;
			panelInput.add(btnBrowseInput, gbc_btnBrowseInput);

			JButton btnLoad = new JButton("Load");
			GridBagConstraints gbc_btnLoad = new GridBagConstraints();
			gbc_btnLoad.fill = GridBagConstraints.BOTH;
			gbc_btnLoad.ipadx = 5;
			gbc_btnLoad.ipady = 5;
			gbc_btnLoad.insets = new Insets(0, 0, 0, 5);
			gbc_btnLoad.gridx = 1;
			gbc_btnLoad.gridy = 1;
			panelInput.add(btnLoad, gbc_btnLoad);
			btnLoad.addActionListener((a) -> {
				if (txtInputFile.getText().trim().length() == 0)
					JOptionPane.showMessageDialog(this, "You need to enter an input file");
				else
					load(new File(txtInputFile.getText()));
			});
			final JFileChooser chooseInput = new JFileChooser();
			chooseInput.setFileFilter(new FileNameExtensionFilter("Toshi Model Driver", "tmd"));
			chooseInput.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooseInput.setDialogTitle("Open Model File");
			btnBrowseInput.addActionListener((a) -> {
				if (sourceFile != null && sourceFile.exists())
					chooseInput.setSelectedFile(sourceFile);
				else if (sourceFile != null && sourceFile.getParentFile() != null
						&& sourceFile.getParentFile().exists())
					chooseInput.setCurrentDirectory(sourceFile.getParentFile());
				else {
					File defdir = SharedUIOptions.instance().file(SharedUIOptions.SOURCE_DIR);
					if (defdir != null) {
						File models = new File(defdir, "Models");
						if (models.exists() && models.isDirectory()) {
							chooseInput.setCurrentDirectory(models);
						}
					}
				}

				if (chooseInput.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					txtInputFile.setText(chooseInput.getSelectedFile().getAbsolutePath());
				}
			});
		}
		{
			JPanel panelInput = new JPanel();
			entireTop.add(panelInput);
			GridBagLayout gbl_panelInput = new GridBagLayout();
			gbl_panelInput.columnWidths = new int[] { 45, 405, 0, 0 };
			gbl_panelInput.rowHeights = new int[] { 23, 20, 0 };
			gbl_panelInput.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
			gbl_panelInput.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			panelInput.setLayout(gbl_panelInput);

			JLabel lblInputFile = new JLabel("Collada File");
			GridBagConstraints gbc_lblInputFile = new GridBagConstraints();
			gbc_lblInputFile.weighty = 1.0;
			gbc_lblInputFile.fill = GridBagConstraints.BOTH;
			gbc_lblInputFile.insets = new Insets(0, 0, 5, 5);
			gbc_lblInputFile.gridx = 0;
			gbc_lblInputFile.gridy = 0;
			panelInput.add(lblInputFile, gbc_lblInputFile);

			txtColladaFile = new JTextField();
			GridBagConstraints gbc_txtInputFile = new GridBagConstraints();
			gbc_txtInputFile.insets = new Insets(0, 0, 5, 5);
			gbc_txtInputFile.weighty = 1.0;
			gbc_txtInputFile.weightx = 1.0;
			gbc_txtInputFile.fill = GridBagConstraints.BOTH;
			gbc_txtInputFile.gridx = 1;
			gbc_txtInputFile.gridy = 0;
			panelInput.add(txtColladaFile, gbc_txtInputFile);
			txtColladaFile.setColumns(10);

			JButton btnBrowseInput = new JButton("...");
			GridBagConstraints gbc_btnBrowseInput = new GridBagConstraints();
			gbc_btnBrowseInput.weighty = 1.0;
			gbc_btnBrowseInput.fill = GridBagConstraints.BOTH;
			gbc_btnBrowseInput.insets = new Insets(0, 0, 5, 0);
			gbc_btnBrowseInput.gridx = 2;
			gbc_btnBrowseInput.gridy = 0;
			panelInput.add(btnBrowseInput, gbc_btnBrowseInput);

			JButton btnLoad = new JButton("Load");
			GridBagConstraints gbc_btnLoad = new GridBagConstraints();
			gbc_btnLoad.fill = GridBagConstraints.BOTH;
			gbc_btnLoad.ipadx = 5;
			gbc_btnLoad.ipady = 5;
			gbc_btnLoad.insets = new Insets(0, 0, 0, 5);
			gbc_btnLoad.gridx = 1;
			gbc_btnLoad.gridy = 1;
			panelInput.add(btnLoad, gbc_btnLoad);
			btnLoad.addActionListener((a) -> {
				if (txtColladaFile.getText().trim().length() == 0)
					JOptionPane.showMessageDialog(this, "You need to enter an input Collada file");
				else
					loadCollada(new File(txtColladaFile.getText()));
			});
			final JFileChooser chooseInput = new JFileChooser();
			chooseInput.setFileFilter(new FileNameExtensionFilter("Collada Model", "dae"));
			chooseInput.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooseInput.setDialogTitle("Open Collada File");
			btnBrowseInput.addActionListener((a) -> {
				if (colladaFile != null && colladaFile.exists())
					chooseInput.setSelectedFile(colladaFile);
				else if (colladaFile != null && colladaFile.getParentFile() != null
						&& colladaFile.getParentFile().exists())
					chooseInput.setCurrentDirectory(colladaFile.getParentFile());
				else {
					File defdir = SharedUIOptions.instance().file(SharedUIOptions.SOURCE_DIR);
					if (defdir != null) {
						File models = new File(defdir, "Models");
						if (models.exists() && models.isDirectory()) {
							chooseInput.setCurrentDirectory(models);
						}
					}
				}

				if (chooseInput.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
					txtColladaFile.setText(chooseInput.getSelectedFile().getAbsolutePath());
				}
			});
		}
		{
			JPanel panelOutput = new JPanel();
			add(panelOutput, BorderLayout.SOUTH);
			GridBagLayout gbl_panelOutput = new GridBagLayout();
			gbl_panelOutput.columnWidths = new int[] { 51, 397, 0, 0 };
			gbl_panelOutput.rowHeights = new int[] { 23, 20, 0 };
			gbl_panelOutput.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
			gbl_panelOutput.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
			panelOutput.setLayout(gbl_panelOutput);

			JLabel lblOutputFile = new JLabel("Output File");
			GridBagConstraints gbc_lblOutputFile = new GridBagConstraints();
			gbc_lblOutputFile.weighty = 1.0;
			gbc_lblOutputFile.fill = GridBagConstraints.BOTH;
			gbc_lblOutputFile.insets = new Insets(0, 0, 5, 5);
			gbc_lblOutputFile.gridx = 0;
			gbc_lblOutputFile.gridy = 0;
			panelOutput.add(lblOutputFile, gbc_lblOutputFile);

			txtOutputFile = new JTextField();
			GridBagConstraints gbc_txtOutputFile = new GridBagConstraints();
			gbc_txtOutputFile.insets = new Insets(0, 0, 5, 5);
			gbc_txtOutputFile.weighty = 1.0;
			gbc_txtOutputFile.weightx = 1.0;
			gbc_txtOutputFile.fill = GridBagConstraints.BOTH;
			gbc_txtOutputFile.gridx = 1;
			gbc_txtOutputFile.gridy = 0;
			panelOutput.add(txtOutputFile, gbc_txtOutputFile);

			JButton btnBrowseOutput = new JButton("...");
			GridBagConstraints gbc_btnBrowseOutput = new GridBagConstraints();
			gbc_btnBrowseOutput.weighty = 1.0;
			gbc_btnBrowseOutput.fill = GridBagConstraints.BOTH;
			gbc_btnBrowseOutput.insets = new Insets(0, 0, 5, 0);
			gbc_btnBrowseOutput.gridx = 2;
			gbc_btnBrowseOutput.gridy = 0;
			panelOutput.add(btnBrowseOutput, gbc_btnBrowseOutput);

			btnSave = new JButton("Save");
			GridBagConstraints gbc_btnSave = new GridBagConstraints();
			gbc_btnSave.fill = GridBagConstraints.BOTH;
			gbc_btnSave.ipady = 5;
			gbc_btnSave.ipadx = 5;
			gbc_btnSave.insets = new Insets(0, 0, 0, 5);
			gbc_btnSave.gridx = 1;
			gbc_btnSave.gridy = 1;
			panelOutput.add(btnSave, gbc_btnSave);
			btnSave.addActionListener((a) -> {
				if (txtOutputFile.getText().trim().length() == 0)
					JOptionPane.showMessageDialog(this, "You need to enter an output file");
				else
					save(new File(txtOutputFile.getText()));
			});
			final JFileChooser chooseOutput = new JFileChooser();
			chooseOutput.setFileFilter(new FileNameExtensionFilter("Collada Model", "dae"));
			chooseOutput.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooseOutput.setDialogTitle("Save Model File");
			btnBrowseOutput.addActionListener((a) -> {
				if (destFile != null && destFile.exists())
					chooseOutput.setSelectedFile(destFile);
				else {
					if (destFile != null && destFile.getParentFile() != null && destFile.getParentFile().exists())
						chooseOutput.setCurrentDirectory(destFile.getParentFile());
					else {
						File defdir = SharedUIOptions.instance().file(SharedUIOptions.DEST_DIR);
						if (defdir != null && defdir.exists()) {
							File models = new File(defdir, "Models");
							if (!models.isFile()) {
								models.mkdirs();
								chooseOutput.setCurrentDirectory(models);
							}
						}
					}
					if (sourceFile != null && sourceFile.exists()) {
						File cwd = chooseOutput.getCurrentDirectory();
						if (cwd != null) {
							chooseOutput.setSelectedFile(new File(cwd, sourceFile.getName()));
						}
					}
				}

				if (chooseOutput.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
					txtOutputFile.setText(chooseOutput.getSelectedFile().getAbsolutePath());
				}
			});
		}

		lblStats = new JLabel("");
		add(new JScrollPane(lblStats), BorderLayout.CENTER);

		btnSave.setEnabled(true);
	}

	private void loadCollada(File f) {
		try {
			colladaIn = ModelMerger_DAE.loadScene(f);
		} catch (Exception e) {
			colladaIn = null;
			UIUtil.alert("Failed to load", e, this);
		}
	}

	private void load(File f) {
		try {
			model = new TMD_File(f);
			lblStats.setText("<html>" + f.getName() + "<br>" + model.summary().replace("\n", "<br>") + "</html>");
		} catch (Exception e) {
			model = null;
			lblStats.setText("");
			UIUtil.alert("Failed to load", e, this);
		}
		btnSave.setEnabled(model != null);
	}

	private void save(File f) {
		if (model == null)
			JOptionPane.showMessageDialog(this, "No model loaded");
		else if (colladaIn == null)
			JOptionPane.showMessageDialog(this, "No collada file loaded");
		else
			try {
				ModelMerger_DAE merge = new ModelMerger_DAE(model, colladaIn);
				merge.apply();
				ByteBuffer buffer = ByteBuffer.allocate(model.length()).order(ByteOrder.LITTLE_ENDIAN);
				model.write(buffer);
				buffer.flip();
				Utils.write(f, buffer);
			} catch (Exception e) {
				UIUtil.alert("Failed to merge", e, this);
			}
	}
}
