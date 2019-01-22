package com.fxj.simpleplayer01;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/*参考链接:https://www.jianshu.com/p/ec5fd369c518*/

public class SimplePlayer {

    private static final String TAG =SimplePlayer.class.getSimpleName();
    private static final boolean VERBOSE = true;
    private static final long TIMEOUT_US = 10000;

    private IPlayStateListener mListener;
    private VideoDecodeThread mVideoDecodeThread;
    private AudioDecodeThread mAudioDecodeThread;
    private boolean isPlaying;
    private boolean isPause;
    private String filePath;
    private Surface surface;

    // 是否取消播放线程
    private boolean cancel = false;

    public SimplePlayer(Surface surface, String filePath) {
        this.surface = surface;
        this.filePath = filePath;
        isPlaying = false;
        isPause = false;
        displayCodecs();
    }

    /**
     * 设置回调
     * @param mListener
     */
    public void setPlayStateListener(IPlayStateListener mListener) {
        this.mListener = mListener;
    }

    interface IPlayStateListener{
        void videoAspect(int width,int height,float time);
    }
    /**
     * 是否处于播放状态
     * @return
     */
    public boolean isPlaying() {
        return isPlaying && !isPause;
    }

    /**
     * 开始播放
     */
    public void play() {
        isPlaying = true;
        if (mVideoDecodeThread == null) {
            mVideoDecodeThread = new VideoDecodeThread();
            mVideoDecodeThread.start();
        }
        if (mAudioDecodeThread == null) {
            mAudioDecodeThread = new AudioDecodeThread();
            mAudioDecodeThread.start();
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        isPause = true;
    }

    /**
     * 继续播放
     */
    public void continuePlay() {
        isPause = false;
    }

    /**
     * 停止播放
     */
    public void stop() {
        isPlaying = false;
    }

    /**
     * 销毁
     */
    public void destroy() {
        stop();
        if (mAudioDecodeThread != null) {
            mAudioDecodeThread.interrupt();
        }
        if (mVideoDecodeThread != null) {
            mVideoDecodeThread.interrupt();
        }
    }

    /**
     * 解复用，得到需要解码的数据,喂数据
     * @param extractor
     * @param decoder
     * @param inputBuffers
     * @return
     */
    private static boolean decodeMediaData(MediaExtractor extractor, MediaCodec decoder, ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US);/*从MediaCodec中得到一个可以使用的InputBuffer的索引*/
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];/**根据索引得到一个ByteBuffer*/
            int sampleSize = extractor.readSampleData(inputBuffer, 0);/*把指定通道中的数据按偏移量读取到ByteBuffer中,即将MediaExtractor中的未解码的视频数据写入到ByteBuffer类型的inputBuffer*/
            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);/*将inputBufferInde中未解码的数据放入到MediaCodec中供MediaCodec处理*/
                isMediaEOS = true;
                if (VERBOSE) {
                    Log.d(TAG, "end of stream");
                }
            } else {
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();/*读取下一帧数据*/
            }
        }
        return isMediaEOS;
    }

    /**
     * 解码延时
     * @param bufferInfo
     * @param startMillis
     */
    private void decodeDelay(MediaCodec.BufferInfo bufferInfo, long startMillis) {
        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * 获取媒体类型的轨道
     * @param extractor
     * @param mediaType
     * @return
     */
    private static int getTrackIndex(MediaExtractor extractor, String mediaType) {
        int trackIndex = -1;
        Log.d(TAG,"**getTrackIndex**track Count="+extractor.getTrackCount());
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            Log.d(TAG,"**getTrackIndex**mime="+mime);
            if (mime.startsWith(mediaType)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }

    /**
     * 视频解码线程
     */
    private class VideoDecodeThread extends Thread {
        @Override
        public void run() {
            MediaExtractor videoExtractor = new MediaExtractor();
            MediaCodec videoCodec = null;
            try {
                videoExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int videoTrackIndex;
            // 获取视频所在轨道
            videoTrackIndex = getTrackIndex(videoExtractor, "video/");
            if (videoTrackIndex >= 0) {
                MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
                int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
                if (mListener != null) {
                    mListener.videoAspect(width, height, time);
                }
                videoExtractor.selectTrack(videoTrackIndex);
                try {
                    videoCodec = createCodecByMediaFormat(mediaFormat)/*MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME))*/;
                    videoCodec.configure(mediaFormat, surface, null, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (videoCodec == null) {
                if (VERBOSE) {
                    Log.d(TAG, "video decoder is unexpectedly null");
                }
                return;
            }

            videoCodec.start();
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
            boolean isVideoEOS = false;

            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted() && !cancel) {
                if (isPlaying) {
                    // 暂停
                    if (isPause) {
                        continue;
                    }
                    // 将资源传递到解码器
                    if (!isVideoEOS) {
                        isVideoEOS = decodeMediaData(videoExtractor, videoCodec, inputBuffers);
                    }
                    // 从MediaCodec中过去已经解码好的一帧数据的索引index
                    int outputBufferIndex = videoCodec.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_US);
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                            }
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                            }
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            }
                            break;
                        default:
                            // 延迟解码
                            decodeDelay(videoBufferInfo, startMs);
                            // 传入解码好的一帧数据的索引,MediaCodec将释放outputBuffer到供SufaceView显示
                            videoCodec.releaseOutputBuffer(outputBufferIndex, true);
                            break;
                    }
                    // 结尾
                    if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.v(TAG, "buffer stream end");
                        break;
                    }
                }
            }
            // 释放解码器
            videoCodec.stop();
            videoCodec.release();
            videoExtractor.release();
        }
    }
    /**打印所有的编解码器*/
    private void displayCodecs() {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);//REGULAR_CODECS参考api说明,获取常用的编解码器
        MediaCodecInfo[] codecs = list.getCodecInfos();
        for (MediaCodecInfo codec : codecs) {
            if (codec.isEncoder()){
                Log.d(TAG,"**displayCodecs**encoder:"+codec.getName());
            }else{
                Log.i(TAG, "**displayCodecs**decoder:"+codec.getName());
            }

        }
    }

    private MediaCodec createCodecByMediaFormat(MediaFormat format){
        MediaCodecList list=new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        String decoderName =list.findDecoderForFormat(format);
        Log.d(TAG,"**createCodecByMediaFormat**decoderName="+decoderName);
        MediaCodec decoder=null;

        try {
            if(!TextUtils.isEmpty(decoderName)){
                decoder=MediaCodec.createByCodecName(decoderName);
            }

        }catch (IOException e){
            Log.e(TAG,"****createCodecByMediaFormat****IOException Message:"+e.getMessage());
        }
        return decoder;
    }
    /**
     * 音频解码线程
     */
    private class AudioDecodeThread extends Thread {
        private int mInputBufferSize;
        private AudioTrack audioTrack;

        @Override
        public void run() {
            MediaExtractor audioExtractor = new MediaExtractor();
            MediaCodec audioCodec = null;
            try {
                audioExtractor.setDataSource(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < audioExtractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = audioExtractor.getTrackFormat(i);
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i);
                    int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT);
                    int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    mInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
                    int frameSizeInBytes = audioChannels * 2;
                    mInputBufferSize = (mInputBufferSize / frameSizeInBytes) * frameSizeInBytes;
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            audioSampleRate,
                            (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT,
                            mInputBufferSize,
                            AudioTrack.MODE_STREAM);
                    audioTrack.play();
                    try {
                        audioCodec = MediaCodec.createDecoderByType(mime);
                        audioCodec.configure(mediaFormat, null, null, 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }

            if (audioCodec == null) {
                if (VERBOSE) {
                    Log.d(TAG, "audio decoder is unexpectedly null");
                }
                return;
            }
            audioCodec.start();
            final ByteBuffer[] buffers = audioCodec.getOutputBuffers();
            int sz = buffers[0].capacity();
            if (sz <= 0) {
                sz = mInputBufferSize;
            }
            byte[] mAudioOutTempBuf = new byte[sz];

            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = audioCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = audioCodec.getOutputBuffers();
            boolean isAudioEOS = false;
            long startMs = System.currentTimeMillis();
            while (!Thread.interrupted() && !cancel) {
                if (isPlaying) {
                    // 暂停
                    if (isPause) {
                        continue;
                    }
                    // 解码
                    if (!isAudioEOS) {
                        isAudioEOS = decodeMediaData(audioExtractor, audioCodec, inputBuffers);
                    }
                    // 获取解码后的数据
                    int outputBufferIndex = audioCodec.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_US);
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
                            }
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_TRY_AGAIN_LATER");
                            }
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = audioCodec.getOutputBuffers();
                            if (VERBOSE) {
                                Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                            }
                            break;
                        default:
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                            // 延时解码，跟视频时间同步
                            decodeDelay(audioBufferInfo, startMs);
                            // 如果解码成功，则将解码后的音频PCM数据用AudioTrack播放出来
                            if (audioBufferInfo.size > 0) {
                                if (mAudioOutTempBuf.length < audioBufferInfo.size) {
                                    mAudioOutTempBuf = new byte[audioBufferInfo.size];
                                }
                                outputBuffer.position(0);
                                outputBuffer.get(mAudioOutTempBuf, 0, audioBufferInfo.size);
                                outputBuffer.clear();
                                if (audioTrack != null)
                                    audioTrack.write(mAudioOutTempBuf, 0, audioBufferInfo.size);
                            }
                            // 释放资源
                            audioCodec.releaseOutputBuffer(outputBufferIndex, false);
                            break;
                    }

                    // 结尾了
                    if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) {
                            Log.d(TAG, "BUFFER_FLAG_END_OF_STREAM");
                        }
                        break;
                    }
                }
            }

            // 释放MediaCode 和AudioTrack
            audioCodec.stop();
            audioCodec.release();
            audioExtractor.release();
            audioTrack.stop();
            audioTrack.release();
        }

    }
}


