import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;
import javax.swing.SwingWorker;
class Section {
	private byte[] music = null;
	private int readBytes;
	public Section(byte[] music, int readBytes) {
		this.music = music;
		this.readBytes = readBytes;
	}
	
	public byte[] getMusic() {
		return this.music;
	}
	
	public int getByteCount() {
		return this.readBytes;
	}
}

class AudioIOWorker extends SwingWorker<Void, Void> {
	private PlaySound playSound;
	private ArrayBlockingQueue<Section> queue;
	private AudioInputStream audioInputStream;
	public AudioIOWorker(PlaySound playSound, ArrayBlockingQueue<Section> queue, AudioInputStream audioInputStream) {
		this.playSound = playSound;
		this.queue = queue;
		this.audioInputStream = audioInputStream;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		int readBytes = 0;
		
		try {
		    while (readBytes != -1) {
		    	byte[] audioBuffer = new byte[PlaySound.EXTERNAL_BUFFER_SIZE];
				readBytes = audioInputStream.read(audioBuffer, 0,
					audioBuffer.length);
				queue.put(new Section(audioBuffer, readBytes));
				MediaPlayer.fileSize += readBytes;
		    }
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
		System.out.println(MediaPlayer.fileSize);
		try {
			playSound.bufferedIn.close();
			audioInputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized(playSound.getLock()) {
			playSound.deadThread += 1;
		}
		return null;
	}
	
	public void shutDown() {
		synchronized(playSound.getLock()) {
			this.queue.clear();
			try {
				this.queue.put(new Section(new byte[255], -1));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			playSound.deadThread += 1;
		}
	}
}

class PlayWorker extends SwingWorker<Void, Void> {
	private ArrayBlockingQueue<Section> queue;
	private SourceDataLine dataLine;
	private PlaySound playSound;
	public PlayWorker(PlaySound playSound, ArrayBlockingQueue<Section> queue, SourceDataLine dataLine) {
		this.playSound = playSound;
		this.queue = queue;
		this.dataLine = dataLine;
	}

	@Override
	protected Void doInBackground() throws Exception {
		// TODO Auto-generated method stub
		dataLine.start();
		while (true) {
			Section cur = null;
			try {
				synchronized (playSound.getLock()) {
					cur = queue.take();
				}
				if (cur.getByteCount() == -1) {
					break;
				} else {
					synchronized (playSound.getLock()) {
						while(playSound.isPause) {
							playSound.getLock().wait();
							try {
								Thread.sleep(200);//Not sure, 100 ms ~ 200 ms are all acceptable
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					dataLine.write(cur.getMusic(), 0, cur.getByteCount());
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		dataLine.drain();
	    dataLine.close();
	    synchronized(playSound.getLock()) {
			playSound.deadThread += 1;
		}
		return null;
	}
	
	public void shutDown() {
//		shutdown this SwingWorker by putting a "poison pill" into the queue
		synchronized(playSound.getLock()) {
			playSound.deadThread += 1;
		}
	}
}

public class PlaySound {

    private InputStream waveStream;
    private ArrayBlockingQueue<Section> queue;
    public static final int EXTERNAL_BUFFER_SIZE = 9600;//100ms
    public final Object lock = new Object();
    public boolean isPause;
    public SourceDataLine dataLine;
    public AudioIOWorker audioIOWorker;
    public PlayWorker playWorker;
    public InputStream bufferedIn = null;
    public AudioInputStream audioInputStream = null;
    public int deadThread = 0;
    
    /**
     * CONSTRUCTOR
     */
    public PlaySound(InputStream waveStream) {
    	this.waveStream = waveStream;
    	this.queue = new ArrayBlockingQueue<Section>(100);
    	this.isPause = false;
    }

    public void play() throws PlayWaveException {

		try {
			bufferedIn = new BufferedInputStream(this.waveStream);
		    audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
			
		} catch (UnsupportedAudioFileException e1) {
		    throw new PlayWaveException(e1);
		} catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}
	
		// Obtain the information about the AudioInputStream
		AudioFormat audioFormat = audioInputStream.getFormat();
		Info info = new Info(SourceDataLine.class, audioFormat);
	
		// opens the audio channel
		dataLine = null;
		try {
		    dataLine = (SourceDataLine) AudioSystem.getLine(info);
		    dataLine.open(audioFormat, EXTERNAL_BUFFER_SIZE);
		} catch (LineUnavailableException e1) {
		    throw new PlayWaveException(e1);
		}
	
		// Starts the music :P
		this.audioIOWorker = new AudioIOWorker(this, queue, audioInputStream);
		this.audioIOWorker.execute();
		this.playWorker = new PlayWorker(this, queue, dataLine);
		this.playWorker.execute();
    }
    
    public void shutDown() {
    	try {
    		this.bufferedIn.close();
			this.audioInputStream.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	this.audioIOWorker.shutDown();
    	this.playWorker.shutDown();
    	this.audioIOWorker.cancel(true);
    	this.playWorker.cancel(true);
    	while (this.deadThread == 2) {
    		this.audioIOWorker.shutDown();
    		this.playWorker.shutDown();
    		this.audioIOWorker.cancel(true);
    		this.playWorker.cancel(true);
    	}
    }
    
    public Object getLock() {
    	return this.lock;
    }
    
    public void changeStatus() {
    	synchronized(this.lock) {
    		if (!this.isPause) {
    			this.isPause = !this.isPause;
    		} else {
    			this.isPause = !this.isPause;
        		this.lock.notify();
    		}
    	}
    }
}
