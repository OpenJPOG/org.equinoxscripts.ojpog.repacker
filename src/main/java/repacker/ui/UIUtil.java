package repacker.ui;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class UIUtil {
	public static void alert(String title, Throwable t, Component owner) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		alert(sw.toString(), title, owner);
	}

	public static Frame frameOf(Component c) {
		while (c != null && !(c instanceof Frame))
			c = c.getParent();
		return (Frame) c;
	}

	public static void alert(String s, String title, Component owner) {
		JDialog jd = new JDialog(frameOf(owner));
		jd.setTitle(title);
		JTextPane pane = new JTextPane();
		pane.setText(s);
		jd.setSize(500, 500);
		jd.add(new JScrollPane(pane));
		jd.setVisible(true);
		jd.requestFocusInWindow();
	}

	public static GridBagConstraints gbc(int x, int y) {
		return gbc(x, y, 1, 1, 0, 0);
	}

	public static GridBagConstraints gbc(int x, int y, int gw, int gh, double wx, double wy) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = gw;
		gbc.gridheight = gh;
		gbc.weightx = wx;
		gbc.weighty = wy;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = gbc.ipady = 2;
		return gbc;
	}
}
