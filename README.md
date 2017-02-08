# Advertisements-Detection-and-Replacement-in-Multimedia-Content

1. Utilized Java GUI to build a Media Player which has a display panel and three control buttons: Play, Pause and Stop.
Implemented three event handler to correspondingly handle play, pause and stop event.

2. Used Java Swing Worker to synchronously read in video content and audio content.

3. Divided the original content into a series of shots and detected advertisements in these shots based on the feature that
the advertisement frames have low correlation between each other and the content of them changes very quickly.

4. Detected some specific brand images in the video by using OpenCV SIFT library and inserted new advertisements
which are related to these images into the video and audio.
