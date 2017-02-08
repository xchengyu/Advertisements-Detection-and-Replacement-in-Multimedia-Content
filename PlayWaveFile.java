import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class PlayWaveFile {
	public PlaySound playSound;
	public String filename;
	
	public PlayWaveFile(String filename) {
		// opens the inputStream
		this.filename = filename;
	}
	
	public void play() {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
			
		// initializes the playSound Object
		playSound = new PlaySound(inputStream);
		
		// plays the sound
		try {

			playSound.play();
		} catch (PlayWaveException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public PlaySound getPlaySound() {
		return this.playSound;
	}
	
	public void shutDown() {
		this.playSound.shutDown();
	}
}
