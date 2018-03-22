package com.example.administrator.myapplication;

/**
 * Created by Administrator on 2018/3/11.
 */

public class u82uc {
    public static byte[] utf8ToUnicode(String string) {

        byte[] ccc= new byte[string.length()*2+4];
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            Integer in = Integer.valueOf(Integer.toHexString(c),16);
            putShort(ccc,in.shortValue(),i*2);

        }
        ccc[ccc.length-4] = 0x60;
        ccc[ccc.length-3] = (byte)0xa8;
        ccc[ccc.length-2] = 0x59;
        ccc[ccc.length-1] = 0x7D;
        return ccc;
    }

    public static void putShort(byte b[], short s, int index) {
        b[index + 0] = (byte) (s >> 8);
        b[index + 1] = (byte) (s >> 0);
    }


}
