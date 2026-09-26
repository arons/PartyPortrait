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
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
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

	public static final String VERSION = "1.2.0";

	// hard coded config
	private static boolean FULLSCREEN = true;

	// gphoto2 command; override with env GPHOTO2_CMD or -DGPHOTO2_CMD (default works for Nikon D60 and Canon EOS)
	private static final String DEFAULT_GPHOTO2_CMD = "gphoto2 --force-overwrite --capture-image-and-download";
	private static final String gphoto2Cmd = System.getProperty("GPHOTO2_CMD",
			System.getenv().getOrDefault("GPHOTO2_CMD", DEFAULT_GPHOTO2_CMD));

	private static final String fileNamePatter = "yyyy_MM_dd_HHmmss";
	private static final String fileExt = ".jpg";
	private static Object syncObj = new Object();
	

	private Image full = null;
	private Image[][] saver = new Image[3][3];
	private String[] randomPictures = null;
	private int[][] randomOrder = null;
	private int nextPicture = 0, nextPosition = 0;
	private Random r = new Random(1);
	
	private File photoDir;

	private Timer saverTimer = new Timer(2000, new UpdateSaver());

	private boolean running = false;

	private enum Status {
		RANDOM_COLLAGE, FULL_PHOTO, MESSAGE_DISPLAY
	}

	private Status status = Status.MESSAGE_DISPLAY;
	private final static String M_STARTING = "Starting...";

//	private final static String M_LAUGH = "Sorridi!";
//	private final static String M_WAIT = "...un attimo...e...";
	
//	private final static String M_LAUGH = "Ptičica!";
//	private final static String M_WAIT = "...pričekaj...";
	
	private final static String M_LAUGH = "Lächle!";
	private final static String M_WAIT = "...einen Moment...und...";
	
	private String message = M_STARTING;

	private static PartyPictures INSTANCE;
	

	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			if ("-v".equals(arg) || "--version".equals(arg)) {
				System.out.println("PartyPortrait " + VERSION);
				return;
			}
			if ("-h".equals(arg) || "--help".equals(arg)) {
				System.out.println("Usage: java PartyPictures [-v|--version] [-h|--help]");
				System.out.println("  -v, --version   print version and exit");
				return;
			}
		}
		System.out.println("Starting Party Pictures " + VERSION);
		new PartyPictures();
	}

	// Class constructor
	private PartyPictures() throws MalformedURLException {
		INSTANCE = this;
		
		photoDir = new File("photos");
		if(!INSTANCE.photoDir.exists()) {
			INSTANCE.photoDir.mkdirs();
		}
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addMouseListener(new ExitOnMouseClickListener());
		addKeyListener(new TakePhotoOnKeyListener());

		this.setUndecorated(true);
		this.setVisible(true);

		if (FULLSCREEN) {
			GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(this);
		} else {
			this.setSize(600, 400);
		}

		// init random order
		randomOrder = new int[saver.length * saver[0].length][2];
		for (int i = 0; i < saver.length; i++) {
			for (int j = 0; j < saver[0].length; j++) {
				INSTANCE.randomOrder[i * saver.length + j] = new int[] { i, j };
			}
		}

		initPhotoShow();
		System.out.println("running...");
	}

	
	/**
	 * 
	 */
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

	/**
	 * Update by timer
	 */
	private class UpdateSaver implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (randomPictures == null) return; 
			if (randomPictures.length <= 0) return; 
			
			
			String filename = randomPictures[nextPicture++];
			System.out.println("[UpdateSaver] filename:"+filename);
			
			File file = new File(photoDir, filename);
			
			int[] pos = randomOrder[nextPosition++];
			int x = pos[0];
			int y = pos[1];
 
			if (nextPicture >= randomPictures.length)
				nextPicture = 0;

			if (nextPosition >= randomOrder.length)
				nextPosition = 0;

			Image img = Toolkit.getDefaultToolkit().getImage(file.getAbsolutePath());
			
			if(Math.random() > 0.9) {
				full = img.getScaledInstance(getWidth(), -1, Image.SCALE_FAST);
				status = Status.FULL_PHOTO;
				repaint();
				
				saverTimer.stop();
				initPhotoShow();
				
			}else {
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
	
	private void initPhotoShow() {
		System.out.println("initPhotoShow");
		
		synchronized (syncObj) {
			running = false;
			
			/*init saver */
			for (int i = 0; i < saver.length; i++) {
				for (int j = 0; j < saver[i].length; j++) {
					saver[i][j] = null;
				}
			}
			
			
			status = Status.FULL_PHOTO;
			repaint();

			// randomize and initialize random files
			randomPictures = photoDir.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(fileExt);
				}
			});

			Collections.shuffle(Arrays.asList(randomPictures), r);
			Collections.shuffle(Arrays.asList(randomOrder), r);

			saverTimer.start();
		}
		
		
		System.out.println("randomPictures size:"+randomPictures.length);
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
			
			System.out.println("[PhotoThread] taking photo...");
			
			SimpleDateFormat df = new SimpleDateFormat(fileNamePatter);
			String filename = df.format(new Date()) + fileExt;
			File pfile = new File(photoDir, filename);
			System.out.println("[PhotoThread] to file:" + pfile);
			System.out.println("[PhotoThread] cmd:" + gphoto2Cmd);
			
			try {
				Process p = new ProcessBuilder((gphoto2Cmd + " --filename=" + pfile.getAbsolutePath()).split("\\s+"))
						.redirectErrorStream(true).start();
				BufferedReader out = new BufferedReader(new InputStreamReader(p.getInputStream()));

				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = out.readLine()) != null) {
					sb.append(line).append('\n');
					System.out.println("[gphoto2] " + line);
				}

				message = M_LAUGH;
				status = Status.MESSAGE_DISPLAY;
				repaint();
				
				try { Thread.sleep(2000); } catch (InterruptedException ei) { }
				
				message = M_WAIT;
				repaint();
				int rc = p.waitFor();
				System.out.println("[PhotoThread] gphoto2 exit code: " + rc);

				if (rc != 0 || !pfile.exists()) {
					// capture failed (e.g. camera busy, USB timeout, EOS PTP error)
					throw new IOException("gphoto2 failed (rc=" + rc + "): " + sb.toString().trim());
				}

				full = Toolkit.getDefaultToolkit().getImage(pfile.getAbsolutePath());
				full = full.getScaledInstance(getWidth(), -1, Image.SCALE_FAST);

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				message = "Error:"+e.getMessage();
				if (message.length() > 120) message = message.substring(0, 120) + "...";
				status = Status.MESSAGE_DISPLAY;
				repaint();
				try { Thread.sleep(2000); } catch (InterruptedException ei) { }
			} finally {
				initPhotoShow();
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
