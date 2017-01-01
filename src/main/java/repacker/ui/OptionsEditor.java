package repacker.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class OptionsEditor extends JPanel {
	private static final long serialVersionUID = 1L;

	private static abstract class OptKeyValue {
		protected final String keyName;
		protected final JPanel value;
		protected final JLabel label;

		protected OptKeyValue(String keyName, String keyEnglish, String keyTooltip) {
			this.keyName = keyName;
			this.label = new JLabel(keyEnglish);
			this.value = new JPanel();
			this.value.setLayout(new BorderLayout(3, 3));
			this.value.setToolTipText(keyTooltip);
			this.label.setToolTipText(keyTooltip);
		}

		protected abstract void setValue(String value);

		protected abstract String getValue();
	}

	private static class FileKeyValue extends OptKeyValue {
		private final JTextField path;
		private final JButton browserLauncher;
		private final JFileChooser browser;

		protected FileKeyValue(String keyName, String keyEnglish, String keyTooltip, boolean directory) {
			super(keyName, keyEnglish, keyTooltip);
			this.path = new JTextField();
			this.path.setMaximumSize(new Dimension(450, 50));
			this.browser = new JFileChooser();
			this.browser.setFileSelectionMode(directory ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
			this.browserLauncher = new JButton("...");
			this.browserLauncher.addActionListener((a) -> {
				File f = new File(path.getText());
				if (f.exists())
					this.browser.setSelectedFile(f);
				else if (f.getParentFile() != null && f.getParentFile().exists())
					this.browser.setCurrentDirectory(f.getParentFile());
				if (this.browser.showOpenDialog(this.browserLauncher) == JFileChooser.APPROVE_OPTION) {
					this.path.setText(this.browser.getSelectedFile().getAbsolutePath());
				}
			});
			this.value.add(this.path, BorderLayout.CENTER);
			this.value.add(this.browserLauncher, BorderLayout.EAST);
		}

		@Override
		protected void setValue(String value) {
			this.path.setText(value);
		}

		@Override
		protected String getValue() {
			return this.path.getText();
		}

	}

	private final JPanel propsTable;
	private final List<OptKeyValue> props = new ArrayList<>();

	private void add(OptKeyValue p) {
		props.add(p);
		propsTable.add(p.label, UIUtil.gbc(0, props.size(), 1, 1, 0, 0));
		propsTable.add(p.value, UIUtil.gbc(1, props.size(), 1, 1, 1, 0));
	}

	public OptionsEditor() {
		setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(0, 100, 0, 100));
		add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JButton btnSave = new JButton("Save");
		panel.add(btnSave, BorderLayout.EAST);
		btnSave.addActionListener(this::save);

		JButton btnReload = new JButton("Reload");
		panel.add(btnReload, BorderLayout.WEST);
		btnReload.addActionListener(this::load);

		this.propsTable = new JPanel();
		this.propsTable.setLayout(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(propsTable);
		propsTable.add(new JLabel("Name"), UIUtil.gbc(0, 0, 1, 1, 0, 0));
		propsTable.add(new JLabel("Value"), UIUtil.gbc(1, 0, 1, 1, 1, 0));
		add(new FileKeyValue(SharedUIOptions.SOURCE_DIR, "Source Data Directory",
				"The default source directory to use for reading JPOG files.  Typically \"..." + File.separator + "JPOG"
						+ File.separator + "Data\"  This directory will never have JPOG files written to it.",
				true));
		add(new FileKeyValue(SharedUIOptions.DEST_DIR, "Destination Data Directory",
				"The default destination directory to use for writing files.  This directory will never have JPOG files read from it.",
				true));
		scrollPane.setViewportBorder(new LineBorder(new Color(0, 0, 0)));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);
		load(null);
	}

	public void save(ActionEvent e) {
		SharedUIOptions opts = SharedUIOptions.instance();
		Properties dest = opts.properties;
		for (OptKeyValue p : props)
			dest.put(p.keyName, p.getValue());
		try {
			opts.save();
		} catch (IOException t) {
			UIUtil.alert("Failed to save options", t, this);
		}
	}

	public void load(ActionEvent e) {
		SharedUIOptions opts = SharedUIOptions.instance();
		try {
			opts.load();
		} catch (IOException t) {
			UIUtil.alert("Failed to load options", t, this);
		}
		Properties dest = opts.properties;
		for (OptKeyValue p : props) {
			String v = dest.getProperty(p.keyName);
			if (v != null)
				p.setValue(v);
		}
	}
}
