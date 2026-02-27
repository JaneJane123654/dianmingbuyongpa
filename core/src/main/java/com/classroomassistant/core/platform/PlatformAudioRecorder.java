package com.classroomassistant.core.platform;

/**
 * 平台音频录制接口
 * 桌面端使用 javax.sound，安卓端使用 AudioRecord
 */
public interface PlatformAudioRecorder {
    
    /**
     * 开始录音
     * @param listener 音频数据回调
     * @return 是否成功启动
     */
    boolean start(AudioDataListener listener);
    
    /**
     * 停止录音
     */
    void stop();
    
    /**
     * 是否正在录音
     */
    boolean isRecording();
    
    /**
     * 释放资源
     */
    void release();
    
    /**
     * 音频数据回调接口
     */
    interface AudioDataListener {
        /**
         * 接收到音频数据
         * @param data PCM 音频数据
         * @param length 数据长度
         */
        void onAudioData(byte[] data, int length);
        
        /**
         * 录音出错
         * @param error 错误信息
         */
        void onError(String error);
    }
}
