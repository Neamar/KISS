package name.pilgr.pipinyin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is PiPinyin main interface.
 * there are two methods you can call them to convert the chinese to pinyin.
 * PinyinConverter.toPinyin(Context context,char c);
 * PinyinConverter.toPinyin(Context context,String hanzi);
 * <p/>
 */
public class PiPinyin {

    private static final String TAG = PiPinyin.class.getSimpleName();

    private RandomAccessFile mSourceFile = null;

    public PiPinyin(Context context) {
        try {
            mSourceFile = new RandomAccessFile(PinyinDb.readOrCreateFromFile(context), "r");
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't init Pinyin Converter", e);
        }
    }

    public void recycle() {
        try {
            if (mSourceFile != null) {
                mSourceFile.close();
            }
        } catch (IOException e) {
        }
    }

    /**
     * Convert chinese char to pinyin
     *
     * @param c the chinese character
     * @return pinyin string
     */
    public String toPinyin(char c) {
        if (mSourceFile == null) {
            return "";
        }
        if (c == 0x3007) return "ling";
        if (c < 0x4E00 || c > 0x9FA5) {
            return "";
        }
        try {
            long sp = (c - 0x4E00) * 6;
            mSourceFile.seek(sp);
            byte[] buf = new byte[6];
            mSourceFile.read(buf);
            return new String(buf).trim();
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean isChineseChar(char c) {
        if (c == 0x3007) return true;
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * Convert chinese to pinyin
     *
     * @param hanzi the chinese string
     * @return string array with short pinyin[0] and full pinyin[1]
     */
    public String toPinyin(String hanzi, String separator) {
        StringBuilder sbFull = new StringBuilder();
        final List<String> pinyinList = getPinyinListForHanzi(hanzi);
        boolean withSeparator = !TextUtils.isEmpty(separator);

        for (int i = 0; i < pinyinList.size(); i++) {
            String s = pinyinList.get(i);
            if (TextUtils.isEmpty(s)) continue;

            sbFull.append(s);
            if (withSeparator && i < pinyinList.size() - 1) {
                sbFull.append(separator);
            }
        }
        return sbFull.toString();
    }

    public String toShortPinyin(String hanzi, String separator) {
        StringBuilder sbShort = new StringBuilder();
        final List<String> pinyinList = getPinyinListForHanzi(hanzi);
        boolean withSeparator = !TextUtils.isEmpty(separator);

        for (int i = 0; i < pinyinList.size(); i++) {
            String s = pinyinList.get(i);
            if (TextUtils.isEmpty(s)) continue;

            sbShort.append(s.substring(0, 1));
            if (withSeparator && i < pinyinList.size() - 1) {
                sbShort.append(separator);
            }
        }
        return sbShort.toString();
    }

    private List<String> getPinyinListForHanzi(String hanzi) {
        List<String> pinyinList = new ArrayList<String>();

        for (int i = 0; i < hanzi.length(); i++) {
            char ch = hanzi.charAt(i);
            final String pinyin = toPinyin(ch);
            if (!TextUtils.isEmpty(pinyin)) {
                pinyinList.add(pinyin);
            } else {
                pinyinList.add(String.valueOf(ch));
            }
        }
        return pinyinList;
    }
}
