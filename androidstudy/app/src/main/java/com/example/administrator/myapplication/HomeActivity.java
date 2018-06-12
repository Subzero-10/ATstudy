package com.example.administrator.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Objects;

import com.example.administrator.myapplication.Ints;


/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.0
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class HomeActivity extends Activity {


    private static final String TAG = "HomeActivity";
    private static final String UART_DEVICE_NAME = "UART0";
    private static final String GPIO_NAME ="BCM5";
    private static final String GPIO_NAME1 ="BCM6";
    private static final String I2C_DEVICE_NAME ="I2C1";
    private Gpio mGpio;
    private Gpio mGpio1;
    public UartDevice mDevice;
    private Ds3231 mDeviceI;

    String checkTextEnd = null;
    int md5Result = 0;
    String userName = null;
    String userInfo2 = null;
    String userInfo1 = null;
    String userInfo= null;          

    private static final int USB_VENDOR_ID = 1027;
    private static final int USB_PRODUCT_ID = 24577;//ch340 vid pid6790 29987
    private static final int USB_VENDOR_ID2 = 1027;
    private static final int USB_PRODUCT_ID2= 24596;//ft232 vid pid 1027 24577 24599
    private static final int USB_VENDOR_ID3 = 1027;
    private static final int USB_PRODUCT_ID3= 24597;//pl2303 vid pid 1659 8963

    private UsbManager usbManager;
    private UsbDeviceConnection connection;
    private UsbDeviceConnection connection2;
    private UsbDeviceConnection connection3;
    private UsbSerialDevice serialDevice;
    private UsbSerialDevice serialDevice2;
    private UsbSerialDevice serialDevice3;
    public  static ICdbHelper icdb;
    private String buffer;
    private byte[] databyte;
    private int datacount = 0;

    //private AwesomenessCounter mAwesomenessCounter;
    //private final GattServer mGattServer = new GattServer();


    private UsbSerialInterface.UsbReadCallback callback = new UsbSerialInterface.UsbReadCallback() {
        // USB转串口回调程序
        @Override
        public void onReceivedData(byte[] data) {
            Log.i(TAG, "进来了!!!! " );
            // 如果是传进来的第一个byte数组，保留为databyte，否则与之前的databyte拼接为一个，解决FT232传送数据不连续的问题。
            try {
                if (datacount ==0) {
                    databyte = data;
                    datacount = 1;
                }
                else
                    databyte = byteMerger(databyte,data);
                // 将byte数组转换为UTF8编码的string
                String dataUtf8;//修改
                dataUtf8 = new String(databyte, "UTF-8");//修改

                buffer = dataUtf8;//修改
                // 若以\n换行结尾，保留此次数据
                int index;
                Log.i(TAG, "call data is      : " + buffer+"call data is      : " +bytesToHexString(databyte));
                while ((index = buffer.indexOf('\n')) != -1) {
                    datacount = 0;
                    final String dataStr = buffer.substring(0, index + 1).trim();
                    buffer = buffer.length() == index ? "" : buffer.substring(index + 1);
                    final byte[] databytefinal = databyte.clone();
                    Log.i(TAG, "final data is     : " +bytesToHexString(databytefinal));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                // 对接收的数据进行处理
                                onSerialDataReceived(dataStr,databytefinal);
                                databyte = null;
                            } catch (ParseException | IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Error receiving USB data", e);
            }
        }
    };







    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        buffer = "";
        PeripheralManager manager = PeripheralManager.getInstance();
        // 打印串口
        List<String> deviceList = manager.getUartDeviceList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No UART port available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        // 打开串口
        Log.d(TAG, "onCreate: 1");
        try {
            mDevice = manager.openUartDevice(UART_DEVICE_NAME);
            configureUartFrame(mDevice);
            Log.d(TAG, "success!!!!");
        } catch (IOException e) {
            Log.w(TAG, "Unable to access UART device", e);
        }

        // 打开GPIO
        try {
            mGpio = manager.openGpio(GPIO_NAME);
            configureOutputLow(mGpio);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access GPIO", e);
        }
        try {
            mGpio1 = manager.openGpio(GPIO_NAME1);
            configureOutputLow(mGpio1);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access GPIO1", e);
        }

        // 打印I2C
        List<String> deviceList1 = manager.getI2cBusList();
        if (deviceList1.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList1);
        }

        usbManager = getSystemService(UsbManager.class);
        // 打印USB
        Map<String, UsbDevice> connectedDevices = null;
        if (usbManager != null) {
            connectedDevices = usbManager.getDeviceList();
        }
        if (connectedDevices != null) {
            if (connectedDevices.isEmpty()) {
                Log.i(TAG, "No USB available on this device.");
            } else {
                Log.i(TAG, "List of available devices: " + connectedDevices);
            }
        }



        // 初始化DS3231时钟模块，第一次使用或没电时须打开注释重新设置时间
        //Date date;
        try {
            mDeviceI = new Ds3231(I2C_DEVICE_NAME);
            Log.d(TAG, "isTimekeepingDataValid = " + mDeviceI.isTimekeepingDataValid());
            Log.d(TAG, "isOscillatorEnabled = " + mDeviceI.isOscillatorEnabled());

            //Calendar calendar = Calendar.getInstance();
            //calendar.set(2018, Calendar.JUNE, 12,10,56,00);

            //date = calendar.getTime();

            //Log.d(TAG, "DateTime = " + date.toString());
            //mDeviceI.setTime(date);

            //noinspection ConstantConditions
            Log.d(TAG, "getTime = " + mDeviceI.getTime().toString());
            //mDeviceI.setTime(date.getTime());
            Log.d(TAG, "getTime = " + mDeviceI.getTime().getHours());

            //mDeviceI.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while opening screen", e);
            throw new RuntimeException(e);
        }

        icdb= new ICdbHelper(this);

        //mAwesomenessCounter = new AwesomenessCounter(this);

        /*mGattServer.onCreate(this, new GattServer.GattServerListener() {
            @Override
            public byte[] onCounterRead() {
                return Ints.toByteArray(mAwesomenessCounter.getCounterValue());
            }

            @Override
            public void onInteractorWritten() {
                int count = mAwesomenessCounter.incrementCounterValue();
                //mLuckyCat.movePaw();
                //mLuckyCat.updateCounter(count);
            }
        });*/


    }
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "success2");
        Log.d(TAG, "aaaaaaaaaaaaaa!!!!"+md5Result);

        // 打开两个USB转串口
        startUsbConnection();
        startUsbConnection2();
        startUsbConnection3();

        // 程序启动时播报“程序启动”
        try {
            Thread.currentThread();
            Thread.sleep(3000);//阻断3// 秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] textBuf0 ={0x7a,0x0b,0x5e,(byte)0x8f,0x54,0x2f,0x52,(byte)0xa8};
        try {
            writeUartData(mDevice,textBuf0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.currentThread();
            Thread.sleep(1500);//阻断1.5// 秒
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String ip;
        ConnectivityManager conMann = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);

        assert conMann != null;
        NetworkInfo wifiNetworkInfo = conMann.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if(wifiNetworkInfo.isConnected())
        {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            assert wifiManager != null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();
            ip = intToIp(ipAddress);
            System.out.println("wifi_ip地址为------"+ip);

            byte[] textBufip = u82uc.utf8ToUnicode(ip, 10,10);
            try {
                writeUartData(mDevice, textBufip );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //读卡器发送指令
        if(serialDevice3!= null) {
            byte[] textBuf1 = {0x00};
            serialDevice3.write(textBuf1);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    protected void onDestroy() {
        super.onDestroy();
        // 关闭GPIO I2C USB UART连接
        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
        if (mGpio != null) {
            try {
                mGpio.close();
                mGpio = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close GPIO", e);
            }
        }
        if (mGpio1 != null) {
            try {
                mGpio1.close();
                mGpio1 = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close GPIO1", e);
            }
        }

        if (mDeviceI != null) {
            try {
                mDeviceI.close();
                mDeviceI = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close I2C", e);
            }
        }

        stopUsbConnection();

        //mGattServer.onDestroy();

    }



    public void configureUartFrame(UartDevice uart) throws IOException {
        // 配置串口
        uart.setBaudrate(9600);
        uart.setDataSize(8);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(1);
    }

    public void writeUartData(UartDevice uart,byte[] tmpBuf) throws IOException {
        //对语音合成模块发送数据，使用UNICODE编码大头存储
        byte[] SoundBuf = new byte[tmpBuf.length+21];
        int i;
        SoundBuf[0]=(byte)0xFD;
        SoundBuf[1]=0x00;
        SoundBuf[2]=(byte)(tmpBuf.length+18);//数据长度
        SoundBuf[3]=0x01;
        SoundBuf[4]=0x03;//UNICODE编码
        SoundBuf[6]=0x00;
        SoundBuf[5]='[';
        SoundBuf[8]=0x00;
        SoundBuf[7]='v';
        SoundBuf[10]=0x00;
        SoundBuf[9]=0x38;//8级音量
        //SoundBuf[12]=0x00;
        //SoundBuf[11]=0x30;
        SoundBuf[12]=0x00;
        SoundBuf[11]=']';
        SoundBuf[14]=0x00;
        SoundBuf[13]='[';
        SoundBuf[16]=0x00;
        SoundBuf[15]='r';//下一个字强制姓氏发音
        SoundBuf[18]=0x00;
        SoundBuf[17]=0x31;
        SoundBuf[20]=0x00;
        SoundBuf[19]=']';
        for (i=0;i<tmpBuf.length;i++)
        {
            //对发送数据调整为大头存储
            if (i%2 ==0)
                SoundBuf[21+i+1]=tmpBuf[i];
            else
                SoundBuf[21+i+-1]=tmpBuf[i];
        }
        //串口发送数据
        int count = uart.write(SoundBuf, SoundBuf.length);
        Log.d(TAG, "Wrote " + count + " bytes to peripheral");
    }

    public void configureOutputHigh(Gpio gpio) throws IOException {
        //GPIO口拉高
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio.setActiveType(Gpio.ACTIVE_HIGH);
        gpio.setValue(true);
    }
    public void configureOutputLow(Gpio gpio) throws IOException {
        //GPIO口拉低
        // Initialize the pin as a high output
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        // Low voltage is considered active
        gpio.setActiveType(Gpio.ACTIVE_LOW);
        // Toggle the value to be LOW
        gpio.setValue(true);
    }

    private void startUsbConnection() {
        //打开USB连接
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();

        if (!connectedDevices.isEmpty()) {
            for (UsbDevice device : connectedDevices.values()) {
                if (device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                    Log.i(TAG, "Device found: " + device.getDeviceName());
                    startSerialConnection(device);
                    return;
                }
            }
        }
        Log.w(TAG, "Could not start USB connection - No devices found");
    }
    private void startUsbConnection2() {
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();

        if (!connectedDevices.isEmpty()) {
            for (UsbDevice device : connectedDevices.values()) {
                if (device.getVendorId() == USB_VENDOR_ID2 && device.getProductId() == USB_PRODUCT_ID2) {
                    Log.i(TAG, "Device found: " + device.getDeviceName());
                    startSerialConnection2(device);
                    return;
                }
            }
        }
        Log.w(TAG, "Could not start USB connection2 - No devices found");
    }
    private void startUsbConnection3() {
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();

        if (!connectedDevices.isEmpty()) {
            for (UsbDevice device : connectedDevices.values()) {
                if (device.getVendorId() == USB_VENDOR_ID3 && device.getProductId() == USB_PRODUCT_ID3) {
                    Log.i(TAG, "Device found: " + device.getDeviceName());
                    startSerialConnection3(device);
                    return;
                }
            }
        }
        Log.w(TAG, "Could not start USB connection3 - No devices found");
    }

    private void startSerialConnection(UsbDevice device) {
        //配置USB转串口，打开连接
        Log.i(TAG, "Ready to open USB device connection");
        connection = usbManager.openDevice(device);
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialDevice != null) {
            if (serialDevice.open()) {
                serialDevice.setBaudRate(9600);
                serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialDevice.setParity(UsbSerialInterface.PARITY_NONE);
                serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialDevice.read(callback);
                Log.i(TAG, "Serial connection opened");
            } else {
                Log.w(TAG, "Cannot open serial connection");
            }
        } else {
            Log.w(TAG, "Could not create Usb Serial Device");
        }
    }
    private void startSerialConnection2(UsbDevice device) {
        //配置USB转串口，打开连接
        Log.i(TAG, "Ready to open USB device connection");
        connection2 = usbManager.openDevice(device);
        serialDevice2 = UsbSerialDevice.createUsbSerialDevice(device, connection2);
        if (serialDevice2 != null) {
            if (serialDevice2.open()) {
                serialDevice2.setBaudRate(9600);
                serialDevice2.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialDevice2.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialDevice2.setParity(UsbSerialInterface.PARITY_NONE);
                serialDevice2.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialDevice2.read(callback);
                Log.i(TAG, "Serial connection opened");
            } else {
                Log.w(TAG, "Cannot open serial connection");
            }
        } else {
            Log.w(TAG, "Could not create Usb Serial Device");
        }
    }
    private void startSerialConnection3(UsbDevice device) {
        //配置USB转串口，打开连接
        Log.i(TAG, "Ready to open USB device connection");
        connection3 = usbManager.openDevice(device);
        serialDevice3 = UsbSerialDevice.createUsbSerialDevice(device, connection3);
        if (serialDevice3 != null) {
            if (serialDevice3.open()) {
                serialDevice3.setBaudRate(9600);
                serialDevice3.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialDevice3.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialDevice3.setParity(UsbSerialInterface.PARITY_NONE);
                serialDevice3.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                serialDevice3.read(callback);
                Log.i(TAG, "Serial connection opened");
            } else {
                Log.w(TAG, "Cannot open serial connection");
            }
        } else {
            Log.w(TAG, "Could not create Usb Serial Device");
        }
    }

    private void onSerialDataReceived(String data,byte[] databy) throws ParseException, IOException {
        //从USB转串口接收数据后进行处理
        // Add whatever you want here
        int gender = 0;
        String byte2String = bytesToHexString(databy);
        //byte bbb [] = {databy[0],databy[1],databyte}
        final byte[] databy1 = databy.clone();
        Log.i(TAG, "Serial data received: " + data+"  "+bytesToHexString(databy));

        if (md5Result == 4)
        {
            if(checkIC(databy)!=0)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                            registerIC(databy1);

                    }
                });

                byte[] textBuf = {0x6c,(byte)0xe8,0x51,(byte)0x8c,0x62,0x10,0x52,(byte)0x9f};
                try {
                    writeUartData(mDevice, textBuf);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                if(Objects.equals(data, "1,石峥,管理员,2050-04-26 15:46:01,注册,ef7b5e4e"))
                {

                    byte[] textBuf = {(byte)0x90, 0x00, 0x51, (byte)0xfa,0x6c, (byte) 0xe8, 0x51, (byte) 0x8c };
                    try {
                        writeUartData(mDevice, textBuf);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    md5Result = 0;
                }
                else {
                    byte[] textBuf = {0x6c, (byte) 0xe8, 0x51, (byte) 0x8c, 0x59, 0x31, (byte) 0x8d, 0x25};
                    try {
                        writeUartData(mDevice, textBuf);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


        }
        else {
            if (Objects.equals(byte2String, "040C0220000400CED2DADEC90A")||Objects.equals(byte2String, "31CED2DADE310A")) {
                data = "34,石峥,参观,2017-07-12 16:45:19,绿城小区,c6f6e1b3";
                databy[0] = 0x1a;
                databy[databy.length-2] = 0x1a;
            }
            else if (Objects.equals(byte2String, "040C022000040026C4AE15880A")||Objects.equals(byte2String, "3126C4AE15310A")) {
                data = "0,李先生,管理员,2018-04-17 14:13:06,科群大厦205室,2c9a06c7";
                databy[0] = 0x1a;
                databy[databy.length-2] = 0x1a;
            }

            Log.i(TAG, "Serial data received1: " + data + "  " + byte2String);

            if (checkIC(databy)!=0) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (searchIC(databy1))
                            md5Result = 3;
                        else
                            md5Result = 9;

                    }
                });

            }
            else
                {
                if (data.length() > 13) {
                    //将二维码中逗号换成空格
                    userInfo1 = data.replace(",", " ");
                    MD5Util md5 = new MD5Util();
                    //md5校验
                    md5Result = md5.md5Check(userInfo1.substring(0, userInfo1.length() - 9), userInfo1.substring(userInfo1.length() - 8, userInfo1.length()));
                    //性别判断

                    if (userInfo1.charAt(0) == '0')
                        gender = 0;
                    else
                        gender = 1;

                    //将二维码信息以空格为分割分为数组
                    String userInfoSp[] = userInfo1.split(" ");
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    if (userInfoSp.length > 6) {
                        String userTime = userInfoSp[3] + " " + userInfoSp[4];
                        Date qrTime = format.parse(userTime);
                        //将二维码中时间与现在时间比对得到时间差
                        //noinspection ConstantConditions
                        long days = (qrTime.getTime() - mDeviceI.getTime().getTime()) / (1000 * 3600 * 24);


                        Log.i(TAG, "shijiancha: " + days);
                        Log.i(TAG, "nowTime: " + mDeviceI.getTime().toString());
                        Log.i(TAG, "qrTime: " + qrTime.toString());

                        Log.i(TAG, "小区名称: " + userInfoSp[5]);
                        if (days >= 0 && userInfoSp[5].equals("注册")) {
                            Log.i(TAG, "  ");
                            md5Result = 2;
                            //通过
                        }
                    }
                }
                else
                    md5Result = 9;
            }
        }
        switch (md5Result) {
            case 0:
                break;
            case 1:
                /* 验证成功
                   将UTF8编码文字转换为UNICODE编码，通过串口输出语音 */
                userName = userInfo1.substring(userInfo1.indexOf(" ") + 1, userInfo1.indexOf(" ", userInfo1.indexOf(" ") + 1));
                byte[] textBuf = u82uc.utf8ToUnicode(userName, gender,mDeviceI.getTime().getHours());
                try {
                    writeUartData(mDevice, textBuf);
                    Log.d(TAG, "通行码验证通过 " + textBuf[0] + textBuf[1] + textBuf[2] + textBuf[3] + userName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //GPIO口拉高，使能继电器
                try {
                    configureOutputHigh(mGpio);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    configureOutputHigh(mGpio1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //继电器吸合2s
                try {
                    Thread.currentThread();
                    Thread.sleep(2000);//阻断3秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //重置各类数据
                checkTextEnd = null;
                userInfo1 = null;
                userInfo = null;
                userInfo2 = null;
                userName = null;

                try {
                    configureOutputLow(mGpio);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    configureOutputLow(mGpio1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                md5Result = 0;
                break;


            case 2:
                byte[] textBuf1 = {(byte)0x8b,(byte)0xf7,0x6c,(byte)0xe8,0x51,(byte)0x8c,0x00,0x49,0x00,0x43,0x53,0x61};
                try {
                    writeUartData(mDevice, textBuf1);
                    Log.d(TAG, "管理员二维码验证通过 " );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                checkTextEnd = null;
                userInfo1 = null;
                userInfo = null;
                userInfo2 = null;
                userName = null;
                md5Result = 4;
                break;

            case 3:
                /* 验证成功
                   将UTF8编码文字转换为UNICODE编码，通过串口输出语音 */
                byte[] textBuf3 = u82uc.utf8ToUnicode("欢迎", 1,mDeviceI.getTime().getHours());
                byte[] textBuf33 = {0x6b,0x22,(byte)0x8f,(byte)0xce,0x51,0x49,0x4e,0x34,textBuf3[6],textBuf3[7],textBuf3[8],textBuf3[9],textBuf3[10],textBuf3[11]};
                try {
                    writeUartData(mDevice, textBuf33);
                    Log.d(TAG, "IC卡验证通过 " );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //GPIO口拉高，使能继电器
                try {
                    configureOutputHigh(mGpio);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    configureOutputHigh(mGpio1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //继电器吸合2s
                try {
                    Thread.currentThread();
                    Thread.sleep(2000);//阻断3秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //重置各类数据
                checkTextEnd = null;
                userInfo1 = null;
                userInfo = null;
                userInfo2 = null;
                userName = null;

                try {
                    configureOutputLow(mGpio);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    configureOutputLow(mGpio1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                md5Result = 0;
                break;

            case 9:
                //验证失败
                md5Result = 0;
                byte[] textBuf9 ={(byte)0x90,0x1a,(byte)0x88,0x4c,0x78,0x01,0x67,0x2a,(byte)0x8b,(byte)0xc6,0x52,0x2b};
                try {
                    writeUartData(mDevice,textBuf9);
                    Log.d(TAG, "非法通行码 " );
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }


    }

    private void stopUsbConnection() {
        //关闭USB连接
        try {
            if (serialDevice != null) {
                serialDevice.close();
            }

            if (connection != null) {
                connection.close();
            }
            if (serialDevice2 != null) {
                serialDevice2.close();
            }

            if (connection2 != null) {
                connection2.close();
            }
            if (serialDevice3 != null) {
                serialDevice3.close();
            }

            if (connection3 != null) {
                connection3.close();
            }
        } finally {
            serialDevice = null;
            connection = null;
            serialDevice2 = null;
            connection2 = null;
            serialDevice3 = null;
            connection3 = null;
        }
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        //将两个byte数组拼接
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    public static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte aBArray : bArray) {
            sTemp = Integer.toHexString(0xFF & aBArray);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public static int checkIC(byte[] IC)
    {
        byte checksum;
        byte i;
        byte[] ICcheck = IC.clone() ;

        checksum = 0;
        if (IC.length >= IC[1]&&IC[1] >=0) {
            for (i = 0; i < (IC[1] - 1); i++) {
                checksum ^= IC[i]; //异或
            }
            IC[IC[1] - 1] = (byte) ~checksum; //按位取反
            if(Arrays.equals(IC, ICcheck))
                return 1;
        }
        if(IC[0] == 0x31 && IC[IC.length - 2] == 0x31)
            return 2;
        return 0;
    }

    public static boolean registerIC(byte[] IC)
    {

        byte checksum;
        byte i;
        byte[] ICcheck = IC.clone() ;


        SQLiteDatabase db = icdb.getWritableDatabase();

        checksum = 0;
        if (IC.length >= IC[1]&&IC[1] >=0) {
            for (i = 0; i < (IC[1] - 1); i++) {
                checksum ^= IC[i]; //异或
            }
            IC[IC[1] - 1] = (byte) ~checksum; //按位取反
            if(Arrays.equals(IC, ICcheck)) {
                byte[] IcId1 = new byte[IC[1] - 8];
                System.arraycopy(IC, 7, IcId1, 0, IC[1] - 8);
                String byte2String = bytesToHexString(IcId1);
                Log.i(TAG, "注册ID: " + byte2String);
                ContentValues values= new ContentValues();
                values.put("icid", byte2String);
                db.insert("ic",null,values);
                return true;
            }
        }
        if(IC[0] == 0x31 && IC[IC.length - 2] == 0x31) {
            byte[] IcId2 = new byte[IC.length - 3];
            System.arraycopy(IC, 1, IcId2, 0, IC.length - 3);
            String byte2String = bytesToHexString(IcId2);
            Log.i(TAG, "注册ID: " + byte2String);
            ContentValues values= new ContentValues();
            values.put("icid", byte2String);
            db.insert("ic",null,values);
            return true;
        }

        return false;


    }

    public static boolean searchIC(byte[] IC)
    {
        boolean searchIdResult = false;
        SQLiteDatabase db = icdb.getWritableDatabase();
        Cursor cursor = db.query("ic",null,null,null,null,null,null);

        byte checksum;
        byte i;
        byte[] ICcheck = IC.clone() ;

        checksum = 0;
        if (IC.length >= IC[1]&&IC[1] >=0) {
            for (i = 0; i < (IC[1] - 1); i++) {
                checksum ^= IC[i]; //异或
            }
            IC[IC[1] - 1] = (byte) ~checksum; //按位取反
            if(Arrays.equals(IC, ICcheck)) {
                byte[] IcId1 = new byte[IC[1] - 8];
                System.arraycopy(IC, 7, IcId1, 0, IC[1] - 8);
                String byte2String = bytesToHexString(IcId1);
                Log.i(TAG, "搜索ID.: " + byte2String);
                if(cursor.moveToFirst()){
                    do{
                        String icid = cursor.getString(cursor.getColumnIndex("icid"));
                        if (byte2String.equals(icid))
                            searchIdResult = true;
                    }while(cursor.moveToNext());
                }
                cursor.close();

            }
        }
        if(IC[0] == 0x31 && IC[IC.length - 2] == 0x31) {
            byte[] IcId2 = new byte[IC.length - 3];
            System.arraycopy(IC, 1, IcId2, 0, IC.length - 3);
            String byte2String = bytesToHexString(IcId2);
            Log.i(TAG, "搜索ID: " + byte2String);
            if(cursor.moveToFirst()){
                do{
                    String icid = cursor.getString(cursor.getColumnIndex("icid"));
                    if (byte2String.equals(icid))
                        searchIdResult = true;
                }while(cursor.moveToNext());
            }
            cursor.close();

        }


        return  searchIdResult;
    }



}

