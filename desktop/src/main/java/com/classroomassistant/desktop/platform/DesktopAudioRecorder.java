package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.audio.AudioFormatSpec;
import com.classroomassistant.core.platform.PlatformAudioRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;

/**
 * 桌面端音频录制器实现
 * 使用 javax.sound.sampled API
 */
public class DesktopAudioRecorder implements PlatformAudioRecorder {
    
    private static final Logger logger = LoggerFactory.getLogger(DesktopAudioRecorder.class);
    
    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private volatile boolean recording = false;
    
    private final AudioFormat audioFormat;
    
    public DesktopAudioRecorder() {
        this.audioFormat = new AudioFormat(
            AudioFormatSpec.SAMPLE_RATE,
            AudioFormatSpec.SAMPLE_SIZE_BITS,
            AudioFormatSpec.CHANNELS,
            AudioFormatSpec.SIGNED,
            AudioFormatSpec.BIG_ENDIAN
        );
    }
    
    @Override
    public boolean start(AudioDataListener listener) {
        if (recording) {
            logger.warn("已经在录音中");
            return false;
        }
        
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(info)) {
                logger.error("不支持的音频格式");
                listener.onError("不支持的音频格式");
                return false;
            }
            
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat, AudioFormatSpec.BUFFER_SIZE);
            targetDataLine.start();
            
            recording = true;
            
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[AudioFormatSpec.BUFFER_SIZE];
                
                while (recording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        listener.onAudioData(buffer, bytesRead);
                    }
                }
            }, "AudioRecorder-Thread");
            
            recordingThread.start();
            logger.info("开始录音");
            return true;
            
        } catch (LineUnavailableException e) {
            logger.error("音频设备不可用", e);
            listener.onError("音频设备不可用: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void stop() {
        recording = false;
        
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("停止录音");
    }
    
    @Override
    public boolean isRecording() {
        return recording;
    }
    
    @Override
    public void release() {
        stop();
        targetDataLine = null;
        recordingThread = null;
    }
}
