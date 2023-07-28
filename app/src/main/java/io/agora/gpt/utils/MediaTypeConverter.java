package io.agora.gpt.utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import io.agora.gpt.utils.ssrc.SSRC;

public class MediaTypeConverter {
    public byte[] convertMp3ToPcm(String inputFilePath, String pcmPath) throws Exception {
        byte[] pcmData = null;
        int mp3SampleRate = 0;
        // 获取MP3文件的输入流
        File inputFile = new File(inputFilePath);
        FileInputStream input = new FileInputStream(inputFile);

        // 创建解码器
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(input.getFD());

        // 找到第一个音频轨道
        int trackIndex = -1;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                trackIndex = i;
                break;
            }
        }
        if (trackIndex == -1) {
            // 没有找到音频轨道
            return null;
        }

        // 获取音频格式
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        mp3SampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        Log.i(Constants.TAG, "mp3 format:" + format.toString());
        // 创建解码器
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null, null, 0);
        codec.start();

        // 获取输入和输出缓冲区
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();

        // 创建PCM文件输出流
        File pcmFile = new File(pcmPath);
        FileOutputStream outputStream = new FileOutputStream(pcmFile);

        // 循环读取MP3数据并解码
        boolean inputDone = false;
        boolean outputDone = false;
        while (!outputDone) {
            if (!inputDone) {
                // 获取输入缓冲区的索引
                int inputIndex = codec.dequeueInputBuffer(10000);
                if (inputIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        // 输入结束标志
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        // 输入有数据
                        long presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }

            // 获取输出缓冲区的索引
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000);
            if (outputIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputIndex];
                byte[] bytes = new byte[bufferInfo.size];
                outputBuffer.get(bytes);
                outputBuffer.clear();
                outputStream.write(bytes);

                if (null == pcmData) {
                    pcmData = new byte[0];
                }
                byte[] temp = new byte[pcmData.length + bytes.length];
                if (pcmData.length != 0) {
                    System.arraycopy(pcmData, 0, temp, 0, pcmData.length);
                }
                System.arraycopy(bytes, 0, temp, pcmData.length, bytes.length);
                pcmData = temp;

                codec.releaseOutputBuffer(outputIndex, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // 输出结束标志
                    outputDone = true;
                }
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = codec.getOutputBuffers();
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // 音频格式变化，获取新的格式
                format = codec.getOutputFormat();
            }
        }

        // 关闭所有流和解码器
        outputStream.close();
        extractor.release();
        codec.stop();
        codec.release();

        if (mp3SampleRate != Constants.STT_SAMPLE_RATE) {
            String sampleChangedFilePath = pcmPath + " _" + Constants.STT_SAMPLE_RATE + ".pcm";
            File beforeSampleChangedFile = new File(pcmPath);
            File sampleChangedFile = new File(sampleChangedFilePath);
            try {
                FileInputStream fileInputStream = new FileInputStream(beforeSampleChangedFile);
                FileOutputStream fileOutputStream = new FileOutputStream(sampleChangedFile);
                /**
                 * sfrq:先前的采样率
                 * dfrq:目标采样率
                 * bps:1:8bit 2:16 3:24 4:32
                 * dbps:目标bit
                 * nch:通道数
                 */
                new SSRC(fileInputStream, fileOutputStream, mp3SampleRate, Constants.STT_SAMPLE_RATE,
                        2,
                        2,
                        Constants.RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL, Integer.MAX_VALUE, 0, 0, true);
                pcmData = Utils.readFileToByteArray(sampleChangedFilePath);
                if (beforeSampleChangedFile.exists()) {
                    beforeSampleChangedFile.delete();
                }
                sampleChangedFile.renameTo(beforeSampleChangedFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return pcmData;
    }
}
