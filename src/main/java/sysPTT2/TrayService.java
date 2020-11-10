package sysPTT2;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.KeyStroke;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;

public class TrayService {

    public static void main(String[] args) throws IOException, LineUnavailableException, InterruptedException {
	/*
	 * This is bad code. You had your fair warning. I am not responsible for any
	 * brain damage done by reading the code below.
	 */

	if (SystemTray.isSupported()) {
	    AppState app = new AppState();
	    String _tmpOutputString;
	    Process p = Runtime.getRuntime().exec("amixer get Capture");
	    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    while ((_tmpOutputString = stdInput.readLine()) != null) {
		System.out.println(_tmpOutputString);
		boolean containsOff = _tmpOutputString.contains("[off]");
		boolean containsOn = _tmpOutputString.contains("[on]");

		if (containsOn) {
		    app.isMicEnabled = true;
		    break;
		} else if (containsOff) {
		    app.isMicEnabled = false;
		    break;
		}
	    }
	    p.waitFor();

	    if (app.isMicEnabled) {
		Process p2 = Runtime.getRuntime().exec("amixer set Capture toggle");
		p2.waitFor();
		app.isMicEnabled = false;
	    }

	    System.out.println("> systray supported on system");
	    SystemTray tray = SystemTray.getSystemTray();
	    logo logo = new logo();
	    TrayIcon trayIcon = new TrayIcon(
		    ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(logo.micDisabledString))),
		    "SysPTT");

	    PopupMenu popup = new PopupMenu();
	    MenuItem defaultItem = new MenuItem("Exit");
	    popup.add(defaultItem);
	    trayIcon.setPopupMenu(popup);

	    HotKeyListener micDisableListener = new HotKeyListener() {
		@Override
		public void onHotKey(HotKey hotKey) {
		    System.out.println("> hotkey released");
		    try {
			app.isHoldingSpace = false;
			Process p = Runtime.getRuntime().exec("amixer set Capture toggle");
			p.waitFor();
			trayIcon.displayMessage("hotkey released", "mic muted", MessageType.INFO);
			trayIcon.setImage(ImageIO
				.read(new ByteArrayInputStream(Base64.getDecoder().decode(logo.micDisabledString))));
		    } catch (Exception e) {
			// TODO: handle exception
		    }
		}
	    };

	    HotKeyListener micEnableListener = new HotKeyListener() {
		@Override
		public void onHotKey(HotKey hotKey) {

		    try {
			if (!app.isHoldingSpace) {
			    System.out.println("> hotkey pressed");
			    Process p = Runtime.getRuntime().exec("amixer set Capture toggle");
			    p.waitFor();

			    trayIcon.displayMessage("hotkey pressed", "mic unmuted", MessageType.INFO);
			    trayIcon.setImage(ImageIO
				    .read(new ByteArrayInputStream(Base64.getDecoder().decode(logo.micEnabledString))));
			    app.isHoldingSpace = true;
			}

		    } catch (Exception e) {
			// TODO: handle exception
		    }

		}
	    };
	    Provider provider = Provider.getCurrentProvider(false);
	    provider.register(KeyStroke.getKeyStroke("INSERT"), micEnableListener);
	    provider.register(KeyStroke.getKeyStroke("released INSERT"), micDisableListener);
	    ActionListener exitListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    System.out.println("> exit");
		    provider.reset();
		    provider.stop();
		    System.exit(0);
		}
	    };

	    defaultItem.addActionListener(exitListener);
	    trayIcon.setImageAutoSize(true);

	    try {
		tray.add(trayIcon);
	    } catch (AWTException e) {
		System.err.println("TrayIcon could not be added.");
	    }

	}
    }

}
