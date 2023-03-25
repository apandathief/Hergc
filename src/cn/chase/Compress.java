package cn.chase;

import java.io.*;

import static cn.chase.BitFile.*;

public class Compress {
    private static int kMerLen = 11;
    private static int min_rep_len = 11;
    private static int kMer_bit_num = 2 * kMerLen;
    private static int hashTableLen = 1 << kMer_bit_num;
    private static int MAX_CHAR_NUM = 1 << 28;
    private static int vec_size = 1 << 20;

    private static String identifier;
    private static int ref_low_vec_len = 0, tar_low_vec_len = 0, line_break_len = 0, other_char_len = 0, N_vec_len = 0, line_len = 0, ref_seq_len = 0, tar_seq_len = 0;
    private static int diff_pos_loc_len, diff_low_vec_len = 0;

    private static char[] ref_seq_code = new char[MAX_CHAR_NUM];
    private static char[] tar_seq_code = new char[MAX_CHAR_NUM];
    private static int[] ref_low_vec_begin = new int[vec_size];
    private static int[] ref_low_vec_length = new int[vec_size];
    private static int[] tar_low_vec_begin = new int[vec_size];
    private static int[] tar_low_vec_length = new int[vec_size];
    private static int[] N_vec_begin = new int[vec_size];
    private static int[] N_vec_length = new int[vec_size];
    private static int[] other_char_vec_pos = new int[vec_size];
    private static char[] other_char_vec_ch = new char[vec_size];
    private static int[] diff_low_vec_begin = new int[vec_size];
    private static int[] diff_low_vec_length = new int[vec_size];
    private static int[] line_start = new int[vec_size];
    private static int[] line_length = new int[vec_size];
    private static int[] diff_pos_loc_begin = new int[vec_size];
    private static int[] diff_pos_loc_length = new int[vec_size];
    private static int[] line_break_vec = new int[1 << 25];
    private static int[] point = new int[hashTableLen];
    private static int[] loc = new int[MAX_CHAR_NUM];
    private static int[] diff_low_loc = new int[vec_size];
    private static char[] mismatched_str = new char[vec_size];

    public static byte integerCoding(char ch) {
        if (ch == 'A') {
            return 0;
        }
        if (ch == 'C') {
            return 1;
        }
        if (ch == 'G') {
            return 2;
        }
        if (ch == 'T') {
            return 3;
        }
        return -1;
    }

    public static void extractRefInfo(File refFile) {
        BufferedReader br = null;
        String str;
        int str_length;
        char ch;
        Boolean flag = true;
        int letters_len = 0;

        try {
            br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(refFile))));
            br.readLine();
            while ((str = br.readLine()) != null) {
                str_length = str.length();
                for (int i = 0; i < str_length; i++) {
                    ch = str.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        ch = Character.toUpperCase(ch);

                        if (flag) {
                            flag = false;
                            ref_low_vec_begin[ref_low_vec_len] = letters_len;
                            letters_len = 0;
                        }
                    } else {
                        if (!flag) {
                            flag = true;
                            ref_low_vec_length[ref_low_vec_len++] = letters_len;
                            letters_len = 0;
                        }
                    }

                    if (ch == 'A' || ch == 'C' || ch == 'G' || ch == 'T') {
                        ref_seq_code[ref_seq_len ++] = ch;
                    }

                    letters_len ++;
                }
            }

            if (!flag) {
                ref_low_vec_length[ref_low_vec_len ++] = letters_len;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void extractTarInfo(File tarFile) {
        BufferedReader br = null;
        String str;
        char ch;
        int str_length;
        boolean flag = true, n_flag = false;
        int letters_len = 0, n_letters_len = 0;

        try {
            br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(tarFile))));
            identifier = br.readLine();   //先读取一行基因文件标识

            while ((str = br.readLine()) != null) {
                str_length = str.length();
                for (int i = 0; i < str_length; i++) {
                    ch = str.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        ch = Character.toUpperCase(ch);

                        if (flag) {
                            flag = false;
                            tar_low_vec_begin[tar_low_vec_len] = letters_len;
                            letters_len = 0;
                        }
                    } else {
                        if (!flag) {
                            flag = true;
                            tar_low_vec_length[tar_low_vec_len ++] = letters_len;
                            letters_len = 0;
                        }
                    }
                    letters_len ++;

                    if(ch == 'A'||ch == 'G'||ch == 'C'||ch == 'T') {
                        tar_seq_code[tar_seq_len ++] = ch;
                    } else if(ch != 'N') {
                        other_char_vec_pos[other_char_len] = tar_seq_len;
                        other_char_vec_ch[other_char_len ++] = ch;
                    }

                    if (!n_flag) {
                        if (ch == 'N') {
                            N_vec_begin[N_vec_len] = n_letters_len;
                            n_letters_len = 0;
                            n_flag = true;
                        }
                    } else {
                        if (ch != 'N'){
                            N_vec_length[N_vec_len ++] = n_letters_len;
                            n_letters_len = 0;
                            n_flag = false;
                        }
                    }
                    n_letters_len++;
                }

                line_break_vec[line_break_len ++] = str_length;
            }

            if (!flag) {
                tar_low_vec_length[tar_low_vec_len++] = letters_len;
            }

            if (n_flag) {
                N_vec_length[N_vec_len++] = n_letters_len;
            }

            for (int i = other_char_len - 1; i > 0; i --) {
                other_char_vec_pos[i] -= other_char_vec_pos[i - 1];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (line_break_len > 0) {
            int cnt = 1;
            line_start[line_len] = line_break_vec[0];
            for (int i = 1; i < line_break_len; i ++) {
                if (line_start[line_len] == line_break_vec[i]) {
                    cnt ++;
                } else {
                    line_length[line_len ++] = cnt;
                    line_start[line_len] = line_break_vec[i];
                    cnt = 1;
                }
            }
            line_length[line_len ++] = cnt;
        }
    }

    public static void searchMatchPosVec() {    //二次压缩小写字符二元组
        for (int x = 0; x < tar_low_vec_len; x ++) {
            diff_low_loc[x] = 0;
        }

        int start_position = 0, i = 0;
        out:
        while(i < tar_low_vec_len) {
            for (int j = start_position; j < ref_low_vec_len; j ++) {
                if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])) {
                    diff_low_loc[i] = j;
                    start_position = j + 1;
                    i ++;
                    continue out;
                }
            }
            for (int j = start_position - 1; j > 0; j --) {
                if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])) {
                    diff_low_loc[i] = j;
                    start_position = j + 1;
                    i ++;
                    continue out;
                }
            }
            diff_low_vec_begin[diff_low_vec_len] = tar_low_vec_begin[i];
            diff_low_vec_length[diff_low_vec_len ++] = tar_low_vec_length[i ++];
        }

        //diff_low_loc[i]可能是连续的数字，再次压缩成二元组
        if (tar_low_vec_len > 0) {
            int cnt = 1;
            diff_pos_loc_begin[diff_pos_loc_len] = diff_low_loc[0];
            for (int x = 1; x < tar_low_vec_len; x ++) {
                if ((diff_low_loc[x] - diff_low_loc[x - 1]) == 1) {
                    cnt ++;
                } else {
                    diff_pos_loc_length[diff_pos_loc_len ++] = cnt;
                    diff_pos_loc_begin[diff_pos_loc_len] = diff_low_loc[x];
                    cnt = 1;
                }
            }
            diff_pos_loc_length[diff_pos_loc_len ++] = cnt;
        }
    }

    public static void binaryCoding(Stream stream, int num) {
        int type;

        if (num > MAX_CHAR_NUM) {
            System.out.println("Too large to Write!\n");
            return;
        }

        if (num < 2) {
            type = 1;
            bitFilePutBitsInt(stream, type, 2); //01
            bitFilePutBit(stream, num);
        } else if (num < 262146) {
            type = 1;
            num -= 2;
            bitFilePutBit(stream, type);    //1
            bitFilePutBitsInt(stream, num, 18);
        } else {
            type = 0;
            num -= 262146;
            bitFilePutBitsInt(stream, type, 2); //00
            bitFilePutBitsInt(stream, num, 28);
        }
    }

    public static void saveOtherData(Stream stream) {
        binaryCoding(stream, identifier.length());
        char[] meta_data = identifier.toCharArray();
        for(int i = 0; i < identifier.length(); i ++) {
            bitFilePutChar(stream, meta_data[i]);
        }

        binaryCoding(stream, line_len);
        for (int i = 0; i < line_len; i ++) {
            binaryCoding(stream, line_start[i]);
            binaryCoding(stream, line_length[i]);
        }

        binaryCoding(stream, diff_pos_loc_len);
        for (int i = 0; i < diff_pos_loc_len; i ++) {
            binaryCoding(stream, diff_pos_loc_begin[i]);
            binaryCoding(stream, diff_pos_loc_length[i]);
        }

        binaryCoding(stream, diff_low_vec_len);
        for (int i = 0; i < diff_low_vec_len; i ++) {
            binaryCoding(stream, diff_low_vec_begin[i]);
            binaryCoding(stream, diff_low_vec_length[i]);
        }

        binaryCoding(stream, N_vec_len);
        for (int i = 0; i < N_vec_len; i ++) {
            binaryCoding(stream, N_vec_begin[i]);
            binaryCoding(stream, N_vec_length[i]);
        }

        binaryCoding(stream, other_char_len);
        if (other_char_len > 0) {
            for(int i = 0; i < other_char_len; i ++){
                binaryCoding(stream, other_char_vec_pos[i]);
                bitFilePutChar(stream, other_char_vec_ch[i] - 'A');
            }
        }
    }

    public static void kMerHashingConstruct() {
        int value = 0;
        int step_len = ref_seq_len - kMerLen + 1;
        for (int i = 0; i < hashTableLen; i ++) {
            point[i] = -1;
        }

        for (int k = kMerLen - 1; k >= 0; k --) {
            value <<= 2;
            value += integerCoding(ref_seq_code[k]);
        }
        loc[0] = point[value];
        point[value] = 0;

        int shift_bit_num = (kMerLen * 2 - 2);
        int one_sub_str = kMerLen - 1;
        for (int i = 1; i < step_len; i ++) {
            value >>= 2;
            value += (integerCoding(ref_seq_code[i + one_sub_str])) << shift_bit_num;
            loc[i] = point[value];
            point[value] = i;
        }
    }

    public static void searchMatchSeqCode(Stream stream) {
        int pre_pos = 0, misLen_total = 0;
        int step_len = tar_seq_len - kMerLen + 1;   //step_len = 34170096,有step_len组kMer
        int max_length, max_k;

        int misLen = 0, i, j, k, id, ref_idx, tar_idx, length, cur_pos;
        int tar_value;

        for (i = 0; i < step_len; i ++) {
            tar_value = 0;
            for (j = kMerLen - 1; j >= 0; j --) {
                tar_value <<= 2;
                tar_value += integerCoding(tar_seq_code[i + j]);
            }

            id = point[tar_value];  //研究
            if (id > -1) {  //匹配成功，寻找连续最大匹配
                max_length = -1;
                max_k = -1;

                for (k = id; k != -1; k = loc[k]) {
                    ref_idx = k + kMerLen;
                    tar_idx = i + kMerLen;
                    length = kMerLen;
                    while (ref_idx < ref_seq_len && tar_idx < tar_seq_len && ref_seq_code[ref_idx ++] == tar_seq_code[tar_idx ++]) {
                        length ++;
                    }
                    if (length >= min_rep_len && length > max_length) {
                        max_length = length;
                        max_k = k;
                    }
                }

                if (max_k > -1) {
                    //找到最大匹配后，先将tar之间未匹配的写入文件，包括长度和字符
                    binaryCoding(stream, misLen);
                    if (misLen > 0) {
                        misLen_total += misLen;

                        //2-bit coding for mismatched string
                        for(int m = 0; m < misLen; m ++){
                            int num = integerCoding(mismatched_str[m]);
                            bitFilePutBitsInt(stream, num, 2);
                        }
                        misLen = 0;
                    }

                    cur_pos = max_k - pre_pos;
                    pre_pos = max_k + max_length;
                    if (cur_pos < 0) {
                        cur_pos = -cur_pos;
                        bitFilePutBit(stream, 1);
                    } else {
                        bitFilePutBit(stream, 0);
                    }
                    binaryCoding(stream, cur_pos);
                    binaryCoding(stream, max_length - min_rep_len);
                    i += (max_length - 1);
                    continue;
                }
            }
            mismatched_str[misLen ++] = tar_seq_code[i];
        }

        for(; i < tar_seq_len; i++) {
            System.out.println("hello");
            mismatched_str[misLen ++] = tar_seq_code[i];
        }

        binaryCoding(stream, misLen);
        if (misLen > 0) {
            for(int x = 0; x < misLen; x ++) {
                int num=integerCoding(mismatched_str[i]);
                bitFilePutBitsInt(stream, num, 2);
            }
        }
    }

    public static void main(String[] args) {
        File refFile;
        File tarFile;
        File resultFile;

        refFile = new File(args[0]);
        tarFile = new File(args[1]);
        resultFile = new File(args[2]);


        Stream stream = new Stream(resultFile, 0, 0);

        long startTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();
        extractRefInfo(refFile);
        System.out.println("readRefFile耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        kMerHashingConstruct();
        System.out.println("preProcessRef耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        extractTarInfo(tarFile);
        System.out.println("readTarFile耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        searchMatchPosVec();
        System.out.println("searchMatchPosVec耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        saveOtherData(stream);
        System.out.println("saveOtherData耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        searchMatchSeqCode(stream);
        System.out.println("serarchMatchSeqCode耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        System.out.println("总耗费时间为" + (System.currentTimeMillis() - startTime) + "ms");

        int bitBuffer = stream.getBitBuffer();
        int bitCount = stream.getBitCount();
        if (bitCount != 0)
        {
            bitBuffer <<= (8 - bitCount);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(resultFile, true));
                bos.write(bitBuffer);
                bos.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
