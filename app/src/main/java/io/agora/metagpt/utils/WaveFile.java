package io.agora.metagpt.utils;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class WaveFile {
    private WaveHeader mHeader;
    private RandomAccessFile mFile;
    private int mCurPcmSize;
    
    public WaveFile(int sampleRate, short bitsPerSample, short channels, String savePath) {
        mCurPcmSize = 0;
        int pcmSize = 0;
        mHeader = new WaveHeader(pcmSize);
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        mHeader.fileLength = pcmSize + (44 - 8);
        mHeader.FmtHdrLeth = 16;
        mHeader.BitsPerSample = bitsPerSample;
        mHeader.Channels = channels;
        mHeader.FormatTag = 0x0001;
        mHeader.SamplesPerSec = sampleRate;
        mHeader.BlockAlign = (short)(mHeader.Channels * mHeader.BitsPerSample / 8);
        mHeader.AvgBytesPerSec = mHeader.BlockAlign * mHeader.SamplesPerSec;
        mHeader.DataHdrLeth = pcmSize;

        try {
            File file = new File(savePath);
            if (!file.exists()) {
                boolean ret = file.createNewFile();
                if (!ret) {
                    Log.e(Constants.TAG, "savePcmToWav error: " + savePath);
                }
            }

            mFile = new RandomAccessFile(file, "rw");

            byte[] h = mHeader.getHeader();
            mFile.write(h);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int write(byte[] pcm) {
        if (mFile == null) {
            return -1;
        }

        try {
            mFile.write(pcm);
            mCurPcmSize += pcm.length;
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    public int write(byte[] pcm, int offset, int length) {
        if (mFile == null) {
            return -1;
        }

        try {
            mFile.write(pcm, offset, length);
            mCurPcmSize += length;
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }
        return 0;
    }

    public void close() {
        if (mFile == null) {
            return;
        }
        try {
            mHeader.fileLength = mCurPcmSize + (44 - 8);
            mHeader.DataHdrLeth = mCurPcmSize;

            byte[] h = mHeader.getHeader();
            mFile.seek(0);
            mFile.write(h);

            mFile.close();
            mFile = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCurPcmSize = 0;
    }

    static public void savePcmToWav(InputStream is, String outPath) throws IOException {
        File file = new File(outPath);
        if (!file.exists()) {
            boolean ret = file.createNewFile();
            if (!ret) {
                Log.e(Constants.TAG, "savePcmToWav error: " + outPath);
            }
        }
        RandomAccessFile os = new RandomAccessFile(file, "rw");

        int pcmSize = 0;
        WaveHeader header = new WaveHeader(pcmSize);
        //长度字段 = 内容的大小（PCMSize) + 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = pcmSize + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = 1;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = 16000;
        header.BlockAlign = (short)(header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = pcmSize;

        //获取wav文件头字节数组
        byte[] h = header.getHeader();
        os.write(h);

        byte[] buffer = new byte[1024];
        int read = 0;
        while ((read = is.read(buffer)) != -1) {
            os.write(buffer, 0, read);
            pcmSize += read;
        }
        header.fileLength = pcmSize + (44 - 8);
        header.DataHdrLeth = pcmSize;

        h = header.getHeader();
        os.seek(0);
        os.write(h);

        os.close();
        is.close();
    }
}
