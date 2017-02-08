import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/*
 * VideoPlayer is an UI.
 */
class VideoPlayer {
	public JFrame frame;
	public InputPanel videoPanel;
	public Button play;
	public Button pause;
	public Button stop;
	public AudioWorker audioWorker;
	public VideoIOWorker videoIOWorker;
	public AudioPlayWorker audioPlayWorker;
	public VideoPlayWorker videoPlayWorker;
	public VideoPlayer(AudioWorker audioWorker, ArrayBlockingQueue<Block> queue){
		this.audioWorker = audioWorker;
		this.frame = new JFrame("MediaPlayer");
		frame.setLayout(null);
		frame.setLocation(400, 300);
		frame.setSize(498, 347);
		JPanel root = new JPanel();
		root.setLayout(null);
		root.setLocation(0, 0);
		root.setSize(480, 300);
		frame.add(root);
		this.videoPanel = new InputPanel(queue);
		videoPanel.setLayout(null);
		videoPanel.setLocation(0, 0);
		videoPanel.setSize(480, 270);
		videoPanel.setBackground(Color.WHITE);
		root.add(videoPanel);
		JPanel buttons = new JPanel(new GridLayout(1, 3));
		buttons.setSize(480, 30);
		buttons.setLocation(0, 270);
		root.add(buttons);
		this.play = new Button("Play");
		this.pause = new Button("Pause");
		this.stop = new Button("Stop");
		buttons.add(play);
		buttons.add(pause);
		buttons.add(stop);
		play.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				videoIOWorker = new VideoIOWorker(MediaPlayer.videoFileName);
				videoIOWorker.execute();
				
				audioPlayWorker = new AudioPlayWorker(audioWorker);
				audioPlayWorker.execute();
				
				videoPlayWorker = new VideoPlayWorker(videoPanel);
				videoPlayWorker.execute();
			}});
		
		pause.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				new PauseWorker(pause, videoPanel, audioWorker).execute();
			}});
		
		stop.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				videoPlayWorker.shutDown();
				videoIOWorker.cancel(true);
				audioPlayWorker.shutDown();
				audioPlayWorker.cancel(true);
				videoPlayWorker.cancel(true);
			}});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}

class AudioPlayWorker extends SwingWorker<Void, Void> {
	public AudioWorker audioWorker;
	public AudioPlayWorker(AudioWorker audioWorker) {
		this.audioWorker = audioWorker;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		audioWorker.mp.play();
		return null;
	}
	
	public void shutDown() {
		audioWorker.shutDown();
		audioWorker.cancel(true);
	}
}

class VideoPlayWorker extends SwingWorker<Void, Void> {
	public InputPanel videoPanel;
	public VideoPlayWorker(InputPanel videoPanel) {
		this.videoPanel = videoPanel;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		videoPanel.timer.start();
		return null;
	}
	
	public void shutDown() {
		videoPanel.timer.stop();
//		videoPanel.init = true;
		videoPanel.reset();
	}
	
}

class VideoIOWorker extends SwingWorker<Void, Void> {
	private String filename;
	private ArrayBlockingQueue<Block> queue;
	private int width;
	private int height;
	private ArrayList<Integer> finalStartFrame;
	public VideoIOWorker(String filename) {
		this.filename = filename;
		this.queue = MediaPlayer.queue;
		this.queue.clear();
		this.width = MediaPlayer.width;
		this.height = MediaPlayer.height;
		this.finalStartFrame = new ArrayList<Integer>();
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
    	File file = new File(this.filename);
    	InputStream is = null;
    	int len = 480 * 270 * 3;
    	
    	try {
        	is = new FileInputStream(file);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	
    	
    	ArrayList<Double> redDeltaR = new ArrayList<Double>();
    	ArrayList<Double> greenDeltaR = new ArrayList<Double>();
    	ArrayList<Double> blueDeltaR = new ArrayList<Double>();
    	
		float[] redRowValue = new float[width];
		float[] greenRowValue = new float[width];
		float[] blueRowValue = new float[width];
		
		float[] redRowValue2 = new float[width];
		float[] greenRowValue2 = new float[width];
		float[] blueRowValue2 = new float[width];
		
		int[] count = new int[2];
		float[] rValue = new float[2];
		int index = 0;
		
		while (true) {
			byte[] bytes = new byte[(int)len]; 
			int offset = 0;
			int numRead = 0;
			
			if (index%2 == 0) {
				for (int i = 0; i < width; i++) {
					redRowValue[i] = 0.0f;
					greenRowValue[i] = 0.0f;
					blueRowValue[i] = 0.0f;
				}
			} else{
				for (int i = 0; i < width; i++) {
					redRowValue2[i] = 0.0f;
					greenRowValue2[i] = 0.0f;
					blueRowValue2[i] = 0.0f;
				}
			}
			
			try {
				while (offset < bytes.length && (numRead= is.read(bytes, offset, bytes.length-offset)) >= 0) {
					offset += numRead;
				}
				if (offset >= bytes.length) {
					BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					int ind = 0;
					for(int y = 0; y < height; y++) {
						for(int x = 0; x < width; x++){
							byte a = 0;
							byte r = bytes[ind];
							byte g = bytes[ind+height*width];
							byte b = bytes[ind+height*width*2]; 

							//int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
							int pix = ((a << 24) + (r << 16) + (g << 8) + b);
							img.setRGB(x,y,pix);
							ind++;
							
							if(index%2 == 0){
								redRowValue[x] += r;
								greenRowValue[x] += g;
								blueRowValue[x] += b;
							}else{
								redRowValue2[x] += r;
								greenRowValue2[x] += g;
								blueRowValue2[x] += b;
							}
						}
					}
					Block cur = new Block(img);
					queue.put(cur);
				} else {
//					System.out.println("break: "+ index);
					BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
					Block poison = new Block(img);
					poison.markEnd();
					queue.put(poison);
					break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			for (int i = 0; i < width; i++) {
				if (index % 2 == 0) {
					redRowValue[i] /= height;
					greenRowValue[i] /= height;
					blueRowValue[i] /= height;
				} else {
					redRowValue2[i] /= height;
					greenRowValue2[i] /= height;
					blueRowValue2[i] /= height;
				}
			}

			//red
			if (index >= 1) {
				if (index % 2 == 0) {
					count[1] = CalCount(redRowValue2, redRowValue);
				} else {
					count[0] = CalCount(redRowValue, redRowValue2);
				}
			}
			if (index >= 2) {
				if (index % 2 == 0) {
					rValue[0] = CalR(count[0], count[1]);
				} else {
					rValue[1] = CalR(count[1], count[0]);
				}
			}
			if (index >= 3) {
				double detRedR = 0.0;
				if (index % 2 == 0) {
					detRedR = CalDetR(rValue[1], rValue[0]);
				} else {
					detRedR = CalDetR(rValue[0], rValue[1]);
				}
				redDeltaR.add(detRedR);
			}
			
			//green
			if (index >= 1) {
				if (index % 2 == 0) {
					count[1] = CalCount(greenRowValue2, greenRowValue);
				} else {
					count[0] = CalCount(greenRowValue, greenRowValue2);
				}
			}
			if (index >= 2) {
				if (index % 2 == 0) {
					rValue[0] = CalR(count[0], count[1]);
				} else {
					rValue[1] = CalR(count[1], count[0]);
				}
			}
			if (index >= 3) {
				double detGreenR = 0.0;
				if (index % 2 == 0) {
					detGreenR = CalDetR(rValue[1], rValue[0]);
				} else {
					detGreenR = CalDetR(rValue[0], rValue[1]);
				}
				greenDeltaR.add(detGreenR);
			}
			
			//blue
			if(index >= 1){
				if(index%2 == 0)
					count[1] = CalCount(blueRowValue2, blueRowValue);
				else
					count[0] = CalCount(blueRowValue, blueRowValue2);
			}
			if(index >= 2){
				if(index%2 == 0)
					rValue[0] = CalR(count[0], count[1]);
				else
					rValue[1] = CalR(count[1], count[0]);
			}
			if(index >= 3){
				double detBlueR = 0.0;
				if(index%2 == 0)
					detBlueR = CalDetR(rValue[1], rValue[0]);
				else
					detBlueR = CalDetR(rValue[0], rValue[1]);
				blueDeltaR.add(detBlueR);
			}
			
				
			index++;
//			if(index%1000 == 0)
//				System.out.println("Currently processed frames: "+ index);
			
			
		}
		is.close();		
	
		try {
			PrintWriter out = new PrintWriter("output.txt");
			PrintWriter mean20SecondsOut = new PrintWriter("20SecondsOutput.txt");
			PrintWriter splitOut = new PrintWriter("splitOutput.txt");
			PrintWriter rOut = new PrintWriter("rOutput.txt");
			PrintWriter rExclusivetOut = new PrintWriter("RExclusive.txt");
			PrintWriter finalSplitOut = new PrintWriter("finalSplitOutput.txt");
			
			ArrayList<Integer> meanR = new ArrayList<Integer>();
			ArrayList<Integer> meanRStartFrameIndex = new ArrayList<Integer>();
			
			for (int i = 0; i < redDeltaR.size(); i++) {
				double finalDetR = redDeltaR.get(i)+ greenDeltaR.get(i)+blueDeltaR.get(i);
				int frameNumber = i+4;
				int second = (int)((double)frameNumber/30.0);
				
				if(finalDetR >= 0){
					out.printf("Delta R Value: "+ finalDetR + ", Frame: "+ frameNumber + ", Second: "+ second +"\n");
					
					double mean = 0;
					for(int j = 0; j < 15; j++){
						double sum = 0;
						for(int k = 0; k < 30; k++){
							int curCalFrame = i+j*30+k;
							if(curCalFrame >= redDeltaR.size())
								break;
							sum += redDeltaR.get(curCalFrame)+ greenDeltaR.get(curCalFrame)+blueDeltaR.get(curCalFrame);
						}
						mean = mean/(j+1)*j+sum/30/(j+1);
						
						if(j == 14){
							int int_mean = (int) mean;
							meanR.add(int_mean);
							meanRStartFrameIndex.add(frameNumber);
							mean20SecondsOut.printf("Mean Delta R: "+ mean +", Start frame: "+ frameNumber +", End frame: "+ (frameNumber+j*30+29));
							mean20SecondsOut.printf(", Start Second: "+ second +", End Second:"+ (second+j+1) +"\n");
						}
					}
				}
	        }
			
			for(int i = 0; i < meanR.size(); i++)
				rOut.printf(meanR.get(i)+"\n");
			
			//remove redundant value
			ArrayList<Integer> meanRExclusive = new ArrayList<Integer>();
			ArrayList<Integer> meanRStartFrameIndexExclusive = new ArrayList<Integer>();
			meanRExclusive.add(meanR.get(0));
			meanRStartFrameIndexExclusive.add(meanRStartFrameIndex.get(0));
			for(int i = 1; i < meanR.size(); i++){
				int cur = meanR.get(i);
				int pre = meanR.get(i-1);
				if(cur != pre){
					meanRExclusive.add(cur);
					meanRStartFrameIndexExclusive.add(meanRStartFrameIndex.get(i));
				}
			}
			
			for(int i = 0; i < meanRExclusive.size(); i++)
				rExclusivetOut.printf(meanRExclusive.get(i)+"\n");
			
			//derivative
			int[] meanRForDerivative = new int[2];
			ArrayList<Integer> localMaxValue = new ArrayList<Integer>();
			ArrayList<Integer> localMaxValueStartFrameIndex = new ArrayList<Integer>();
			
			meanRForDerivative[1] = meanRExclusive.get(0);
			for(int i = 1; i < meanRExclusive.size(); i++){
				meanRForDerivative[0] = meanRForDerivative[1];
				meanRForDerivative[1] = meanRExclusive.get(i)-meanRExclusive.get(i-1);
				
				if(meanRForDerivative[0] > 0 && meanRForDerivative[1] < 0){
					localMaxValue.add(meanRExclusive.get(i-1));
					localMaxValueStartFrameIndex.add(meanRStartFrameIndexExclusive.get(i-1));
				}
			}
			
			
			for(int i = 0; i < localMaxValue.size(); i++)
				splitOut.printf(localMaxValue.get(i)+"\n");
			
			//threshold = 30, more higher, less slots will be judged as commercial
			ArrayList<Integer> finalLocalMaxValue = new ArrayList<Integer>();
			int indexFinalLocal = -1;
			boolean isCommerical = false;
			for(int i = 0; i < localMaxValue.size(); i++){
				int localMax = localMaxValue.get(i);
				if(localMax > 30){
					if(isCommerical == false){
						indexFinalLocal++;
						isCommerical = true;
						//System.out.println("add: "+localMax);
						finalLocalMaxValue.add(localMax);
					}
					
					
					if(localMax > finalLocalMaxValue.get(indexFinalLocal))
						finalLocalMaxValue.set(indexFinalLocal, localMax);
				}
				else
					isCommerical = false;
			}
			
			for(int i = 0; i < finalLocalMaxValue.size(); i++){
				//System.out.println("final: "+finalLocalMaxValue.get(i));
				finalSplitOut.printf(finalLocalMaxValue.get(i)+"\n");
			}
			
			for(int i = 0; i < finalLocalMaxValue.size(); i++){
				int curFinalLocalMaxValue = finalLocalMaxValue.get(i);
				for(int j = 0; j < localMaxValue.size(); j++){
					int curLocalMaxValue = localMaxValue.get(j);
					if(curFinalLocalMaxValue == curLocalMaxValue)
						finalStartFrame.add(localMaxValueStartFrameIndex.get(j));
				}
			}
			
			finalSplitOut.close();
			rExclusivetOut.close();
			rOut.close();
			splitOut.close();
			mean20SecondsOut.close();
			out.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
//		for(int i = 0; i < finalStartFrame.size(); i++)
//			System.out.println(finalStartFrame.get(i));		
		return null;
	}

	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		if (MediaPlayer.doOutput.equals("1")) {
			OutputWorker outputWorker = new OutputWorker(finalStartFrame);
			outputWorker.execute();
		}
	}

	public static int CalCount(float[] row1, float[] row2){
		int count = 0;
		for(int i = 0; i < row1.length; i++){
			if(i == 0){
				if(row1[0] == row2[0] || row1[0] == row2[1])
					count++;
			}
			else if(i < row1.length-1){
				if(row1[i] == row2[i-1] || row1[i] == row2[i] || row1[i] == row2[i+1])
					count++;
			}
			else{
				if(row1[i] == row2[i-1] || row1[i] == row2[i])
					count++;
			}
		}
			
		return count;
	}

	public static float CalR(int count1, int count2){
		if(count2 == 0)
			return 0.0f;
		return ((float)count1) / ((float)count2);
	}
	
	public static float CalDetR(float r1, float r2){
		if(Math.abs(r2) <= 0.01)
			return 0.0f;
		return r1/r2;
	}
}

class Block {
	public BufferedImage img;
	public boolean isEnd;
	public Block(BufferedImage img) {
		this.img = img;
		this.isEnd = false;
	}
	public void markEnd() {
		this.isEnd = true;
	}
}

class PauseWorker extends SwingWorker<Void, Void> {
	public static boolean isPause = true;
	public InputPanel videoPanel;
	public Button pause;
	public AudioWorker audioWorker;
	public PauseWorker(Button pause, InputPanel videoPanel, AudioWorker audioWorker) {
		this.pause = pause;
		this.videoPanel = videoPanel;
		this.audioWorker = audioWorker;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		if (PauseWorker.isPause) {
			videoPanel.timer.stop();
			videoPanel.reset();
			PauseWorker.isPause = !PauseWorker.isPause;
			pause.setLabel("Resume");
		} else {
			videoPanel.timer.start();
			PauseWorker.isPause = !PauseWorker.isPause;
			pause.setLabel("Pause");
		}
		audioWorker.getPlayer().getPlaySound().changeStatus();
		return null;
	}
}

/*
 * InputPanel is used to display images 
 */
class InputPanel extends JPanel {
	public int width = MediaPlayer.width;
	public int height =  MediaPlayer.height;
	public ArrayBlockingQueue<Block> queue;
    public Timer timer;
    public long startTime = 0;
    public int count = 0;
    public boolean init;
    public InputPanel(ArrayBlockingQueue<Block> queue) {
    	this.queue = queue;
    	this.init = true;
        timer = new Timer((int)(1000.0f / MediaPlayer.FPS), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               repaint();
            }
        });
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(MediaPlayer.width, MediaPlayer.height);
    }

    
	@Override
	protected void paintComponent(Graphics graphic) {
        super.paintComponent(graphic);
        if (this.init) {
        	init = false;
        } else {
			if (count == 0) {
				startTime = System.currentTimeMillis();
			}
	        Block cur = null;
			try {
				cur = queue.take();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (cur.isEnd) {
				timer.stop();
				return;
			}
			graphic.drawImage(cur.img, 0, 0, this);
			long curTime = System.currentTimeMillis();
			count++;
//			if (count != 1) {
				if ((curTime - startTime) *30/ 1000 - count >= 1) {
					try {
						cur = queue.take();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					graphic.drawImage(cur.img, 0, 0, this);
					count++;
//				}
//				long sum = curTime - startTime;
//				System.out.println(sum / count);
			}
			//System.out.println((curTime - startTime) *30/ 1000 -(count));
        }
    }
	
	public void reset() {
		this.startTime = 0;
		this.count = 0;
	}
}



/*
 * MediaPlayer is used to display images and play music
 */
public class MediaPlayer {
	public static int fileSize;
	public static String videoFileName;
	public static String audioFileName;
	public static String videoOutputName;
	public static String audioOutputName;
	public static String doOutput;
	public static String doInsert;
	public static int width = 480;
	public static int height = 270;
	public static int FPS = 30;
	public static ArrayBlockingQueue<Block> queue;
	public static String logoInfo;
	public static List<String> detectLogoImages;
	public static List<String> insertVideos;
	public static List<String> insertAudios;
	public static int deleteAudio;
//	public static int videoInsertFrameNumber = 15;
//	public static int audioInsertFrameNumber = videoInsertFrameNumber / 3;
	public static String videoInsertOutputName;
	public static String audioInsertOutputName;
	public static void main(String[] args) {
		fileSize = 0;
		if (args.length == 2) {
			videoFileName = args[0];
			audioFileName = args[1];
			doOutput = "0";
			doInsert = "0";
		} else if (args.length == 4) {
			videoFileName = args[0];
			audioFileName = args[1];
			videoOutputName = args[2];
			audioOutputName = args[3];
			doOutput = "1";
			doInsert = "0";
		} else {
			videoFileName = args[0];
			audioFileName = args[1];
			videoOutputName = args[2];
			audioOutputName = args[3];
			logoInfo = args[4];
			doOutput = "1";
			doInsert = "1";
			detectLogoImages = new ArrayList<String>();
			insertVideos = new ArrayList<String>();
			insertAudios = new ArrayList<String>();
			MediaPlayer.parseFile();
		}
		queue = new ArrayBlockingQueue<Block>(100);
		new MediaPlayer();
	}
	
	public MediaPlayer() {
		EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }
                start();
            }
        });
	}
	
	private static void parseFile() {
		File file = new File(MediaPlayer.logoInfo);// Need to be changed to the line below
//		File file = new File(MediaPlayer.logoInfo);// Used on Unix
		BufferedReader fbr = null;
		try {
			fbr = new BufferedReader(new FileReader(file));
			String tempString = null;
            int numOfLogo = 0;
            try {
				if ((tempString = fbr.readLine()) != null) {//must check line number first
					numOfLogo = Integer.parseInt(tempString);	
				}
				
				for (int i = 0; i < 3; i++) {
					int line = 0;
					List<String> curList;
					if (i == 0) {
						curList = MediaPlayer.detectLogoImages;
					} else if (i == 1) {
						curList = MediaPlayer.insertVideos;
					} else {
						curList = MediaPlayer.insertAudios;
					}
					while (line < numOfLogo && ((tempString = fbr.readLine()) != null)) {//must check line number first
						curList.add(tempString);	
					    line++;
					}
				}
				if ((tempString = fbr.readLine()) != null) {//must check line number first
					MediaPlayer.videoInsertOutputName = tempString;	
				}
				if ((tempString = fbr.readLine()) != null) {//must check line number first
					MediaPlayer.audioInsertOutputName = tempString;	
				}
//				checkParameter();// test
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} finally {
			try {
				if (fbr != null) {
					fbr.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
//	private static void checkParameter() {
//		int a = MediaPlayer.detectLogoImages.size();
//		int b = MediaPlayer.insertVideos.size();
//		int c = MediaPlayer.insertAudios.size();
//		if (a != b || b != c || c != a) {
//			System.out.println("wrong");
//		}
//		for (int i = 0; i < a; i++) {
//			System.out.println("picture url: " + MediaPlayer.detectLogoImages.get(i) 
//					+ ", insert Video url: " + MediaPlayer.insertVideos.get(i)
//					+ ", insert Audio url: " + MediaPlayer.insertAudios.get(i));
//		}
//		System.out.println("Video output url: " + MediaPlayer.videoInsertOutputName);
//		System.out.println("Audio output url: " + MediaPlayer.audioInsertOutputName);
//	}
	
	private void start() {
		AudioWorker audioWorker = new AudioWorker(MediaPlayer.audioFileName);
		VideoWorker videoWorker = new VideoWorker(audioWorker, MediaPlayer.queue);
		videoWorker.execute();
		audioWorker.execute();
		while(audioWorker.isDone()) {
			System.out.println("ha");
		}
	}
	
}

class VideoWorker extends SwingWorker<Void, Void> {
	public AudioWorker audioWorker;
	public ArrayBlockingQueue<Block> queue;
	public VideoWorker(AudioWorker audioWorker, ArrayBlockingQueue<Block> queue) {
		this.audioWorker = audioWorker;
		this.queue = queue;
	}
	
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		VideoPlayer vp = new VideoPlayer(this.audioWorker, this.queue);
		return null;
	}
}

class AudioWorker extends SwingWorker<Void, Void> {
	public String filename;
	public PlayWaveFile mp;
	public AudioWorker(String filename) {
		this.filename = filename;
	}
	public PlayWaveFile getPlayer() {
		return mp;
	}
	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		mp = new PlayWaveFile(filename);
		return null;
	}
	
	public void shutDown() {
		mp.shutDown();
	}
}

class OutputWorker extends SwingWorker<Void, Void> {
	private ArrayList<Integer> finalStartFrame;
	private Set<Integer> videoStartFrame;
	private Set<Integer> audioStartFrame;
//	public static final Object lock = new Object();
//	public static int deadThreads;
	public OutputWorker(ArrayList<Integer> finalStartFrame) {
		this.finalStartFrame = finalStartFrame;
		this.videoStartFrame = videoDeleteFrame();
		this.audioStartFrame = audioDeleteFrame();
//		deadThreads = 0;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		new VideoOutputWorker(videoStartFrame).execute();
		new AudioOutputWorker(audioStartFrame).execute();
		return null;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		
		
	}

	private Set<Integer> videoDeleteFrame() {
		Set<Integer> output = new HashSet<Integer>();
		for (Integer num: this.finalStartFrame) {
			for (int i = -30; i < 480; i++) {
				int cur = num + i + 1;
				if (cur > 0) {
					output.add(cur);
				}
			}
		}
		return output;
	}
	
	private Set<Integer> audioDeleteFrame() {
		Set<Integer> output = new HashSet<Integer>();
		for (Integer num: this.finalStartFrame) {
			for (int i = -30; i < 480; i++) {
				int cur = ((num + i) / 3) + 1; 
				if (cur > 0) {
					output.add(cur);
				}
			}
		}
		MediaPlayer.deleteAudio = output.size();
		return output;
	}
	
}

class VideoOutputWorker extends SwingWorker<Void, Void> {
	
	private Set<Integer> finalStartFrame;
	private int width;
	private int height;
	public VideoOutputWorker(Set<Integer> finalStartFrame) {
		this.finalStartFrame = finalStartFrame;
		this.width = MediaPlayer.width;
		this.height = MediaPlayer.height;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		File inputFile = new File(MediaPlayer.videoFileName);
    	InputStream inputStream = null;
//    	File insertFile = new File(MediaPlayer.videoInsertName);
//    	InputStream insertStream = null;
    	FileOutputStream fos = null;
    	int len = width * height * 3;
    	
    	try {
        	inputStream = new FileInputStream(inputFile);
//        	insertStream = new FileInputStream(insertFile);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    	try {
    		fos = new FileOutputStream(MediaPlayer.videoOutputName);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	int time = 0;
    	int count = 0;
    	while (true) {
			byte[] bytes = new byte[(int)len]; 
			int offset = 0;
			int numRead = 0;
			
			try {
				while (offset < bytes.length && (numRead= inputStream.read(bytes, offset, bytes.length-offset)) >= 0) {
					offset += numRead;
				}
				if (offset >= bytes.length) {
					count++;
					if (this.finalStartFrame.contains(count)) {
						time++;
						continue;
					} else {
//						if (count == MediaPlayer.videoInsertFrameNumber) {
//							while (true) {
//								byte[] insertBytes = new byte[(int)len]; 
//								int insertOffset = 0;
//								int insertNumRead = 0;
//								try {
//									while (insertOffset < insertBytes.length && (insertNumRead= insertStream.read(insertBytes, insertOffset, insertBytes.length - insertOffset)) >= 0) {
//										insertOffset += insertNumRead;
//									}
//									if (insertOffset >= insertBytes.length) {
//										fos.write(insertBytes);
//									} else {
//										break;
//									}
//								} catch (IOException e1) {
//									e1.printStackTrace();
//								}	
//							}
//						}
						fos.write(bytes);
					}
				} else {
					break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}	
		}
    	System.out.println("delete: " + time);
//    	insertStream.close();
		inputStream.close();
		fos.close();
		return null;
	}
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		System.out.println("Video has been created!");
		if (MediaPlayer.doInsert.equals("1")) {
			new LogoDetector(MediaPlayer.videoOutputName, MediaPlayer.audioOutputName, MediaPlayer.videoInsertOutputName, 
					MediaPlayer.audioInsertOutputName, MediaPlayer.detectLogoImages, MediaPlayer.insertVideos, 
					MediaPlayer.insertAudios).execute();
		}
	}

}

class AudioOutputWorker extends SwingWorker<Void, Void> {
	private Set<Integer> finalStartFrame;
	public AudioOutputWorker(Set<Integer> finalStartFrame) {
		this.finalStartFrame = finalStartFrame;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		FileInputStream inputStream = null;
	    InputStream bufferedIn = null;
	    AudioInputStream audioInputStream = null;
	    
//		FileInputStream insertStream = null;
//	    InputStream insertBufferedIn = null;
//	    AudioInputStream insertAudioInputStream = null;
		try {
			inputStream = new FileInputStream(MediaPlayer.audioFileName);
			bufferedIn = new BufferedInputStream(inputStream);
		    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
		    
//		    insertStream = new FileInputStream(MediaPlayer.audioInsertName);
//		    insertBufferedIn = new BufferedInputStream(insertStream);
//		    insertAudioInputStream = AudioSystem.getAudioInputStream(insertBufferedIn);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedAudioFileException e1) {
		    throw new PlayWaveException(e1);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
		

		File out = new File(MediaPlayer.audioOutputName);
//		boolean bigEndian = false;
//	    boolean signed = true;
//	    int bits = 16;
//	    int channels = 1;
//	    float sampleRate = 48000;
	    AudioFormat audioFormat = audioInputStream.getFormat();
	    AudioInputStream audioInputStream_ForOutput = null;
//	    format = new AudioFormat((float)sampleRate, bits, channels, signed, bigEndian);
		int readBytes = 0;
		int count = 0;
		int total = 0;
		int time = 0;
		try {
				
		    	byte[] audioBuffer = new byte[MediaPlayer.fileSize];
		    	byte[] junkBuffer = new byte[PlaySound.EXTERNAL_BUFFER_SIZE];
		    	float[] buffer = new float[audioBuffer.length / 2];
		    	int offset = 0;
		    	while (readBytes != -1 && total <= MediaPlayer.fileSize) {
		    		count++;
//					total += readBytes;
					if (finalStartFrame.contains(count)) {
						readBytes = audioInputStream.read(junkBuffer, 0, PlaySound.EXTERNAL_BUFFER_SIZE);
						time++;
					} else {
//						if (count == MediaPlayer.audioInsertFrameNumber) {
//							try {
//						    	while (readBytes != -1) {
////									total += readBytes;
//									readBytes = insertAudioInputStream.read(audioBuffer, offset, PlaySound.EXTERNAL_BUFFER_SIZE);
//									offset += readBytes; 
//						    	}
//							} catch (IOException e1) {
//							    throw new PlayWaveException(e1);
//							}
//						}
						readBytes = audioInputStream.read(audioBuffer, offset, PlaySound.EXTERNAL_BUFFER_SIZE);
						offset += readBytes;
						total += readBytes;					
					}
		    	}
		    	System.out.println("time: " + time);
		    	ByteArrayInputStream bais = new ByteArrayInputStream(audioBuffer);
//		    	readBytes = bais.read(audioBuffer, 0, audioBuffer.length);
		    	audioInputStream_ForOutput = new AudioInputStream(bais, audioFormat, buffer.length);
		    	AudioSystem.write(audioInputStream_ForOutput, AudioFileFormat.Type.WAVE, out);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
		
//		System.out.println("total: " + total);

	    
		audioInputStream_ForOutput.close();
	    audioInputStream.close();
//	    insertAudioInputStream.close();
		return null;
	}
	
//	public int byteArrayToInt(byte[] b) {
//	    return   b[3] & 0xFF |
//	            (b[2] & 0xFF) << 8 |
//	            (b[1] & 0xFF) << 16 |
//	            (b[0] & 0xFF) << 24;
//	}
	
//	public long byteArrayToLong(byte[] bytes) {
//	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
//	    buffer.put(bytes);
//	    buffer.flip();//need flip 
//	    return buffer.getLong();
//	}
	
//	public byte[] intToByteArray(int value) {
//		
//		    return new byte[] {
//		            (byte)(value >>> 24),
//		            (byte)(value >>> 16),
//		            (byte)(value >>> 8),
//		            (byte)value};
//
//    }

    // convert a short to a byte array
//    public byte[] shortToByteArray(short data) {
//        return new byte[]{(byte) (data & 0xff), (byte) ((data >>> 8) & 0xff)};
//    }
	
	@Override
	protected void done() {
		// TODO Auto-generated method stub
		super.done();
		System.out.println("Audio has been created!");
	}
}

