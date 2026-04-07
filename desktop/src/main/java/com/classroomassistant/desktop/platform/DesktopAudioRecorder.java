package com.classroomassistant.desktop.platform;

import com.classroomassistant.core.audio.AudioFormatSpec;
import com.classroomassistant.core.platform.PlatformAudioRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 桌面端音频录制器实现
 * 使用 javax.sound.sampled API
 */
public class DesktopAudioRecorder implements PlatformAudioRecorder {

    private static final Logger logger = LoggerFactory.getLogger(DesktopAudioRecorder.class);

    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private volatile String activeAudioSourceName = "UNKNOWN";
    private PlatformAudioRecorder.AudioDataListener currentListener;

    private final AudioFormat audioFormat;
    private final Object ringBufferLock = new Object();
    private final int maxLookbackBytes = AudioFormatSpec.BYTES_PER_SECOND * 300;
    private final byte[] ringBuffer = new byte[maxLookbackBytes];
    private int ringWritePos;
    private long ringTotalWritten;

    public DesktopAudioRecorder() {
        this.audioFormat = new AudioFormat(
                AudioFormatSpec.SAMPLE_RATE,
                AudioFormatSpec.SAMPLE_SIZE_BITS,
                AudioFormatSpec.CHANNELS,
                AudioFormatSpec.SIGNED,
                AudioFormatSpec.BIG_ENDIAN);
    }

    @Override
    public boolean start(AudioDataListener listener) {
        if (recording.get()) {
            logger.warn("已经在录音中");
            return false;
        }

        this.currentListener = listener;
        synchronized (ringBufferLock) {
            ringWritePos = 0;
            ringTotalWritten = 0L;
        }

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                logger.error("不支持的音频格式");
                listener.onError("不支持的音频格式");
                return false;
            }

            targetDataLine = openPreferredTargetLine(info);
            targetDataLine.open(audioFormat, AudioFormatSpec.BUFFER_SIZE);
            targetDataLine.start();

            recording.set(true);

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[AudioFormatSpec.BUFFER_SIZE];

                while (recording.get()) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        appendToRingBuffer(buffer, bytesRead);
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
        recording.set(false);

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

        currentListener = null;
        logger.info("停止录音");
    }

    @Override
    public boolean isRecording() {
        return recording.get();
    }

    @Override
    public void release() {
        stop();
        targetDataLine = null;
        recordingThread = null;
    }

    public byte[] getAudioBefore(int seconds) {
        int safeSeconds = Math.max(1, Math.min(300, seconds));
        int bytesNeeded = safeSeconds * AudioFormatSpec.BYTES_PER_SECOND;
        synchronized (ringBufferLock) {
            int available = (int) Math.min(ringTotalWritten, maxLookbackBytes);
            if (available <= 0) {
                return new byte[0];
            }
            int readSize = Math.min(bytesNeeded, available);
            int start = (ringWritePos - readSize + maxLookbackBytes) % maxLookbackBytes;

            byte[] result = new byte[readSize];
            if (start + readSize <= maxLookbackBytes) {
                System.arraycopy(ringBuffer, start, result, 0, readSize);
            } else {
                int firstPart = maxLookbackBytes - start;
                System.arraycopy(ringBuffer, start, result, 0, firstPart);
                System.arraycopy(ringBuffer, 0, result, firstPart, readSize - firstPart);
            }
            return result;
        }
    }

    public String getActiveAudioSourceName() {
        return activeAudioSourceName;
    }

    public List<String> queryAudioInputDevices() {
        List<String> devices = new ArrayList<>();
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : infos) {
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                if (mixer.isLineSupported(lineInfo)) {
                    devices.add(info.getName() + " - " + info.getDescription());
                }
            } catch (Exception ignore) {
                // ignore broken mixers
            }
        }
        if (devices.isEmpty()) {
            devices.add("未检测到可用输入设备");
        }
        return devices;
    }

    private void appendToRingBuffer(byte[] data, int length) {
        if (length <= 0) {
            return;
        }
        int safeLength = Math.min(length, data.length);
        synchronized (ringBufferLock) {
            if (safeLength >= maxLookbackBytes) {
                System.arraycopy(data, safeLength - maxLookbackBytes, ringBuffer, 0, maxLookbackBytes);
                ringWritePos = 0;
                ringTotalWritten += safeLength;
                return;
            }

            int firstPart = Math.min(maxLookbackBytes - ringWritePos, safeLength);
            System.arraycopy(data, 0, ringBuffer, ringWritePos, firstPart);
            int secondPart = safeLength - firstPart;
            if (secondPart > 0) {
                System.arraycopy(data, firstPart, ringBuffer, 0, secondPart);
            }
            ringWritePos = (ringWritePos + safeLength) % maxLookbackBytes;
            ringTotalWritten += safeLength;
        }
    }

    private TargetDataLine openPreferredTargetLine(DataLine.Info lineInfo) throws LineUnavailableException {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (!mixer.isLineSupported(lineInfo)) {
                    continue;
                }
                TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                activeAudioSourceName = mixerInfo.getName();
                logger.info("使用音频输入设备: {}", activeAudioSourceName);
                return line;
            } catch (LineUnavailableException ignore) {
                // try next one
            }
        }
        activeAudioSourceName = "DEFAULT";
        return (TargetDataLine) AudioSystem.getLine(lineInfo);
    }
}
