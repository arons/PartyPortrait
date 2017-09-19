
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.Timer;

public class PartyPictures extends JFrame {

	static final long serialVersionUID = 1;

	// hard coded config
	private static boolean FULLSCREEN = true;

	private static final String fileNamePatter = "yyyy_MM_dd_HHmmss";
	private static final String fileExt = ".jpg";

	private Image full = null;
	private Image[][] saver = new Image[3][3];
	private String[] randomPictures = null;
	private int[][] randomOrder = null;
	private int nextPicture = 0, nextPosition = 0;
	private Random r = new Random(1);

	Timer saverTimer = new Timer(2000, new UpdateSaver());

	boolean running = false;

	private enum Status {
		RANDOM_COLLAGE, FULL_PHOTO, MESSAGE_DISPLAY
	}

	private Status status = Status.MESSAGE_DISPLAY;
	private String message = "Starting...";

	private static Object syncObj = new Object();

	public static void main(String[] args) throws Exception {
		new PartyPictures();
	}

	// Class constructor
	private PartyPictures() throws MalformedURLException {

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addMouseListener(new ExitOnMouseClickListener());
		addKeyListener(new TakePhotoOnKeyListener());

		this.setUndecorated(true);
		this.setVisible(true);

		if (FULLSCREEN) {
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		} else {
			this.setSize(300, 200);
		}

		// init random order
		randomOrder = new int[saver.length * saver[0].length][2];
		for (int i = 0; i < saver.length; i++) {
			for (int j = 0; j < saver[0].length; j++) {
				randomOrder[i * saver.length + j] = new int[] { i, j };
			}
		}

		saverTimer.start();
	}

	public void paint(Graphics g) {
		switch (status) {
		case MESSAGE_DISPLAY:
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.BLACK);
			Font font = new Font("San Serif", Font.PLAIN, 60);
			g.setFont(font);
			
			Graphics2D g2d = (Graphics2D) g;
			FontRenderContext frc = g2d.getFontRenderContext();
		    TextLayout layout = new TextLayout(message, font, frc);
		    Rectangle2D bounds = layout.getBounds();

		    int width = (int) Math.round(bounds.getWidth());
		    int height = (int) Math.round(bounds.getHeight());
		    int sx = (getWidth() - width) / 2;
		    int sy = height + (getHeight() - height) / 2;

		    layout.draw(g2d, (float) sx, (float) sy);
		    
			break;
		case FULL_PHOTO:
			if (full != null) {
				g.drawImage(full, 0, 0, this);
			}
			break;
		case RANDOM_COLLAGE:
			if (saver != null) {
				int x = 0, y = 0;
				for (int i = 0; i < saver.length; i++) {
					for (int j = 0; j < saver[i].length; j++) {
						g.drawImage(saver[i][j], x, y, this);
						x = x + getWidth() / saver[i].length;
					}
					x = 0;
					y = y + getHeight() / saver.length;
				}
			}
			break;
		default:
			break;
		}

		

		
	}

	private class UpdateSaver implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			if (randomPictures != null && randomPictures.length > 0) {
				String filename = randomPictures[nextPicture++];

				int[] pos = randomOrder[nextPosition++];
				int x = pos[0];
				int y = pos[1];

				if (nextPicture >= randomPictures.length)
					nextPicture = 0;

				if (nextPosition >= randomOrder.length)
					nextPosition = 0;

				Image img = Toolkit.getDefaultToolkit().getImage(filename);

				img = img.getScaledInstance(getWidth() / saver.length, getHeight() / saver[x].length, Image.SCALE_FAST);
				saver[x][y] = img;
				full = null;
				status = Status.RANDOM_COLLAGE;
				repaint();
			}
		}

	}

	private class ExitOnMouseClickListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			System.exit(0);
		}
	}

	/**
	 * 
	 *
	 */
	private class PhotoThread implements Runnable {

		public void run() {

			synchronized (syncObj) {
				running = true;
			}
			message = "Sorridi";
			status = Status.MESSAGE_DISPLAY;
			repaint();
			

			SimpleDateFormat df = new SimpleDateFormat(fileNamePatter);
			String filename = df.format(new Date()) + fileExt;
			System.out.println("Filename:" + filename);
			try {
				Process p = Runtime.getRuntime().exec("gphoto2 --capture-image-and-download --filename=" + filename);
				message = "...aspetta...";
				repaint();
				p.waitFor();

				full = Toolkit.getDefaultToolkit().getImage(filename);
				full = full.getScaledInstance(getWidth(), -1, Image.SCALE_FAST);

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				message = "Error:"+e.getMessage();
				repaint();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
				}
			} finally {

				synchronized (syncObj) {
					running = false;

					init(saver);
					status = Status.FULL_PHOTO;
					repaint();

					// randomize and initialize random files
					File f = new File(".");
					randomPictures = f.list(new FilenameFilter() {

						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(fileExt);
						}
					});

					Collections.shuffle(Arrays.asList(randomPictures), r);
					Collections.shuffle(Arrays.asList(randomOrder), r);

					saverTimer.start();
				}

			}
		}

		public void init(Image[][] in) {
			for (int i = 0; i < in.length; i++) {
				for (int j = 0; j < in[i].length; j++) {
					in[i][j] = null;
				}
			}
		}
	}

	/**
	 *
	 */
	private class TakePhotoOnKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			System.out.println("Key event received: " + e);
			if (e.getKeyChar() == 'e') {
				System.exit(0);
			}

			synchronized (syncObj) {
				if (running) {
					return;
				}
				saverTimer.stop();
				PhotoThread pt = new PhotoThread();
				new Thread(pt).start();
			}

		}
	}
}
