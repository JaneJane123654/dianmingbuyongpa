package com.classroomassistant.core.audio;

/**
 * 音频格式规格（跨平台通用）
 */
public final class AudioFormatSpec {
    
    /** 采样率: 16kHz */
    public static final int SAMPLE_RATE = 16000;
    
    /** 采样位数: 16 bit */
    public static final int SAMPLE_SIZE_BITS = 16;
    
    /** 声道数: 单声道 */
    public static final int CHANNELS = 1;
    
    /** 是否有符号 */
    public static final boolean SIGNED = true;
    
    /** 字节序: 小端 */
    public static final boolean BIG_ENDIAN = false;
    
    /** 每帧字节数 */
    public static final int FRAME_SIZE = SAMPLE_SIZE_BITS / 8 * CHANNELS;
    
    /** 每秒字节数 */
    public static final int BYTES_PER_SECOND = SAMPLE_RATE * FRAME_SIZE;
    
    /** 默认缓冲区大小（约 100ms） */
    public static final int BUFFER_SIZE = BYTES_PER_SECOND / 10;
    
    private AudioFormatSpec() {}
}
