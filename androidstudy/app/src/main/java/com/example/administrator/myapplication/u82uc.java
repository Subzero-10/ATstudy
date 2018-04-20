package com.example.administrator.myapplication;

/**
 * Created by Administrator on 2018/3/11.
 */

class u82uc {
    static byte[] utf8ToUnicode(String string, int gender, int time) {

        byte[] ccc= new byte[string.length()*2];
        byte[] cccc= new byte[12];
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            Integer in = Integer.valueOf(Integer.toHexString(c),16);
            putShort(ccc,in.shortValue(),i*2);

        }
        cccc[0] = ccc[0];
        cccc[1] = ccc[1];
        if (gender == 0)
        {
            cccc[2] = 0x59;
            cccc[3] = 0x73;
            cccc[4] = 0x58;
            cccc[5] = (byte)0xeb;
        }
        else if (gender == 10)
        {
            return ccc;
        }
        else
        {
            cccc[2] = 0x51;
            cccc[3] = 0x48;
            cccc[4] = 0x75;
            cccc[5] = 0x1f;
        }
        if (time <= 12)
        {
            cccc[6] = 0x4e;
            cccc[7] = 0x0a;
            cccc[8] = 0x53;
            cccc[9] = 0x48;
        }
        else if (time <= 18)
        {
            cccc[6] = 0x4e;
            cccc[7] = 0x0b;
            cccc[8] = 0x53;
            cccc[9] = 0x48;
        }
        else
        {
            cccc[6] = 0x66;
            cccc[7] = 0x5a;
            cccc[8] = 0x4e;
            cccc[9] = 0x0a;
        }
        cccc[cccc.length-2] = 0x59;
        cccc[cccc.length-1] = 0x7D;
        return cccc;
    }

    private static void putShort(byte b[], short s, int index) {
        b[index] = (byte) (s >> 8);
        b[index + 1] = (byte) (s);
    }


}
