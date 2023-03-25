package cn.chase;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitFile {

    public static void bitFilePutBit(Stream stream, int c) {
        BufferedOutputStream bos = null;
        int bitCount = stream.getBitCount();
        int bitBuffer = stream.getBitBuffer();
        bitCount ++;
        bitBuffer <<= 1;
        stream.setBitCount(bitCount);
        stream.setBitBuffer(bitBuffer);

        if (c != 0) {
            bitBuffer |= 1;
            stream.setBitBuffer(bitBuffer);
        }

        if (stream.getBitCount() == 8) {
            try {
                bos = new BufferedOutputStream(new FileOutputStream(stream.getFile(), true));
                bos.write((char)bitBuffer);
                bos.flush();

                stream.setBitCount(0);
                stream.setBitBuffer(0);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void bitFilePutChar(Stream stream, int c) {
        BufferedOutputStream bos = null;
        int bitCount = stream.getBitCount();
        int bitBuffer = stream.getBitBuffer();
        int tmp;

        try {
            bos = new BufferedOutputStream(new FileOutputStream(stream.getFile(), true));

            if (bitCount == 0) {
                bos.write((char) c);
                bos.flush();
                return;
            }

            tmp = c >> bitCount;
            tmp = tmp | getInt(bitBuffer << (8 - bitCount), 3);
            bos.write((char)tmp);
            bos.flush();
            stream.setBitBuffer(c);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getInt(int num, int offset) {
        String numOfString = Integer.toBinaryString(num);
        String result;

        while (numOfString.length() < 32) {
            numOfString = "0".concat(numOfString);
        }
        char data[] = numOfString.toCharArray();
        result = new String(data, (8 * offset), 8);

        return (Integer.parseInt(result, 2));
    }

    public static void bitFilePutBitsInt(Stream stream, int num, int count) {    //num是待写的数据，count是写入数据的长度

        int offset = 3;
        int remaining = count;
        String str = "";

        while (remaining >= 8) {
            bitFilePutChar(stream, getInt(num, offset));    //require confirmation
            remaining -= 8;
            offset --;
        }

        //将剩下来的数据写入压缩文件
        if (remaining != 0) {
            int tmp = getInt(num, offset);

            while(tmp != 0){
                str = tmp % 2 + str;
                tmp = tmp / 2;
            }
            while (str.length() < 8) {
                str = "0".concat(str);
            }

            char[] data = str.toCharArray();
            for (int i = 8 - remaining; i < 8; i ++) {
                bitFilePutBit(stream, Integer.parseInt(String.valueOf(data[i])));
            }
        }
    }
}
