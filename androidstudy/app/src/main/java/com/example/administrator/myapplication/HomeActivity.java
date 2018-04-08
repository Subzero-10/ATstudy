package com.example.administrator.myapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

//import com.leinardi.android.things.driver.ds3231.Ds3231;

import java.util.Calendar;
import java.util.Date;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

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
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private static final int I2C_ADDRESS = 0xd0 ;
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

    private static final int USB_VENDOR_ID = 6790;
    private static final int USB_PRODUCT_ID = 29987;
    private static final int USB_VENDOR_ID2 = 1027;
    private static final int USB_PRODUCT_ID2= 24577;

    private UsbManager usbManager;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialDevice;
    private String buffer = "";
    private byte[] databyte;
    private int datacount = 0;
    private int gpiocheck = 0;

    Handler mHandler = new Handler();
    Runnable r = new Runnable() {

        @Override
        public void run() {
            //do something
            //每隔1s循环执行run方法
            mHandler.postDelayed(this, 1000);
        }
    };

    private UsbSerialInterface.UsbReadCallback callback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] data) {
            try {
                if (datacount ==0) {
                    databyte = data;
                    datacount = 1;
                }
                else
                    databyte = byteMerger(databyte,data);
                String dataUtf8 = new String(databyte, "UTF-8");//修改

                //Log.i(TAG, "data is "+ dataUtf8 +" "+data[0]+" "+data[1]+" "+data[2]+" "+data[3]);
                buffer = dataUtf8;//修改
                int index;
                while ((index = buffer.indexOf('\n')) != -1) {
                    datacount = 0;
                    final String dataStr = buffer.substring(0, index + 1).trim();
                    buffer = buffer.length() == index ? "" : buffer.substring(index + 1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                onSerialDataReceived(dataStr);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
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

    private final BroadcastReceiver usbDetachedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                    Log.i(TAG, "USB device detached");
                    stopUsbConnection();
                }
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Date date;

        PeripheralManager manager = PeripheralManager.getInstance();
        List<String> deviceList = manager.getUartDeviceList();
        if (deviceList.isEmpty()) {
            Log.i(TAG, "No UART port available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList);
        }

        Log.d(TAG, "onCreate: 1");
        try {
            mDevice = manager.openUartDevice(UART_DEVICE_NAME);
            configureUartFrame(mDevice);
            Log.d(TAG, "success!!!!");
        } catch (IOException e) {
            Log.w(TAG, "Unable to access UART device", e);
        }

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

        List<String> deviceList1 = manager.getI2cBusList();
        if (deviceList1.isEmpty()) {
            Log.i(TAG, "No I2C bus available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + deviceList1);
        }




        usbManager = getSystemService(UsbManager.class);

        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        if (connectedDevices.isEmpty()) {
            Log.i(TAG, "No USB available on this device.");
        } else {
            Log.i(TAG, "List of available devices: " + connectedDevices);
        }

        // Detach events are sent as a system-wide broadcast
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(usbDetachedReceiver, filter);

        byte[] textBuf0 ={0x7a,0x0b,0x5e,(byte)0x8f,0x54,0x2f,0x52,(byte)0xa8};
        try {
            writeUartData(mDevice,textBuf0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            mDeviceI = new Ds3231(I2C_DEVICE_NAME);
            Log.d(TAG, "isTimekeepingDataValid = " + mDeviceI.isTimekeepingDataValid());
            Log.d(TAG, "isOscillatorEnabled = " + mDeviceI.isOscillatorEnabled());

            //Calendar calendar = Calendar.getInstance();
            //calendar.set(2018, Calendar.MARCH, 23,20,06,00);

            //date = calendar.getTime();

            //Log.d(TAG, "DateTime = " + date.toString());
            //mDeviceI.setTime(date);
            Log.d(TAG, "getTime = " + mDeviceI.getTime().toString());
            //mDeviceI.setTime(date.getTime());
            Log.d(TAG, "getTime = " + mDeviceI.getTime().toString());

            //mDeviceI.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while opening screen", e);
            throw new RuntimeException(e);
        } //finally {
           // mDeviceI = null;
        //}

    }
    protected void onStart() {
        super.onStart();
        //UsbManager usbManager = getSystemService(UsbManager.class);
        //Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
        //if (connectedDevices.isEmpty()) {
        //    Log.i(TAG, "No UART port available on this device.");
        //} else {
        //    Log.i(TAG, "USBList of available devices: " + connectedDevices);
        //}
        /*CH34xUARTDriver ch340 = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this,
                ACTION_USB_PERMISSION);
        int retval = ch340.ResumeUsbList();
        if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
        {
            Log.d(TAG,"打开设备失败!");
            ch340.CloseDevice();
        } else if (retval == 0) {
            if (!ch340.UartInit()) {//对串口设备进行初始化操作
                Log.d(TAG, "打开设备失败!");
                return;
            }
            Log.d(TAG,"打开设备OK!");
        }
        if (ch340.SetConfig(115200, 8, 1, 0,
                0)) {
            Toast.makeText(MainActivity.this, "串口设置成功!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "串口设置失败!",
                    Toast.LENGTH_SHORT).show();
        }*/
        Log.d(TAG, "success2");
        Log.d(TAG, "aaaaaaaaaaaaaa!!!!"+md5Result);

        startUsbConnection2();
        startUsbConnection();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    protected void onDestroy() {
        super.onDestroy();

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
        unregisterReceiver(usbDetachedReceiver);
        stopUsbConnection();
    }

    public void configureUartFrame(UartDevice uart) throws IOException {
        // Configure the UART port
        uart.setBaudrate(9600);
        uart.setDataSize(8);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(1);
    }

    public void writeUartData(UartDevice uart,byte[] tmpBuf) throws IOException {
        byte[] SoundBuf = new byte[tmpBuf.length+23];
        byte[] SoundBuf2 = new byte[tmpBuf.length+24];
        byte xorcrc = 0;
        int i;
        SoundBuf[0]=(byte)0xFD;
        SoundBuf[1]=0x00;
        SoundBuf[2]=(byte)(tmpBuf.length+20);
        SoundBuf[3]=0x01;
        SoundBuf[4]=0x03;
        SoundBuf[6]=0x00;
        SoundBuf[5]='[';
        SoundBuf[8]=0x00;
        SoundBuf[7]='v';
        SoundBuf[10]=0x00;
        SoundBuf[9]=0x32;
        SoundBuf[12]=0x00;
        SoundBuf[11]=0x30;
        SoundBuf[14]=0x00;
        SoundBuf[13]=']';
        SoundBuf[16]=0x00;
        SoundBuf[15]='[';
        SoundBuf[18]=0x00;
        SoundBuf[17]='r';
        SoundBuf[20]=0x00;
        SoundBuf[19]=0x31;
        SoundBuf[22]=0x00;
        SoundBuf[21]=']';
        for (i=0;i<tmpBuf.length;i++)
        {
            if (i%2 ==0)
                SoundBuf[23+i+1]=tmpBuf[i];
            else
                SoundBuf[23+i+-1]=tmpBuf[i];
        }
        for (i=0;i<tmpBuf.length+23;i++)
        {
            xorcrc=(byte)(xorcrc ^ SoundBuf[i]);
        }
        System.arraycopy(SoundBuf,0,SoundBuf2,0,SoundBuf.length);
        SoundBuf2[SoundBuf2.length-1] = xorcrc;
        int count = uart.write(SoundBuf, SoundBuf.length);
        Log.d(TAG, "Wrote " + count + " bytes to peripheral");
    }

    public void configureOutputHigh(Gpio gpio) throws IOException {
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        gpio.setActiveType(Gpio.ACTIVE_HIGH);
        gpio.setValue(true);
    }
    public void configureOutputLow(Gpio gpio) throws IOException {
        // Initialize the pin as a high output
        gpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
        // Low voltage is considered active
        gpio.setActiveType(Gpio.ACTIVE_LOW);
        // Toggle the value to be LOW
        gpio.setValue(true);
    }
    private void startUsbConnection() {
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
                    startSerialConnection(device);
                    return;
                }
            }
        }
        Log.w(TAG, "Could not start USB connection - No devices found");
    }

    private void startSerialConnection(UsbDevice device) {
        Log.i(TAG, "Ready to open USB device connection");
        connection = usbManager.openDevice(device);
        serialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (serialDevice != null) {
            if (serialDevice.open()) {
                serialDevice.setBaudRate(115200);
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

    private void onSerialDataReceived(String data) throws ParseException, IOException {
        // Add whatever you want here

        Log.i(TAG, "Serial data received: " + data);
        if(data.length()>13) {
            userInfo1 = data.replace(",", " ");
            MD5Util md5 = new MD5Util();
            md5Result = md5.md5Check(userInfo1.substring(0, userInfo1.length() - 9), userInfo1.substring(userInfo1.length() - 8, userInfo1.length()));

            String userInfoSp[] = userInfo1.split(" ");
            SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String userTime = userInfoSp[3]+" "+userInfoSp[4];
            Date qrTime=format.parse(userTime);
            int cha = qrTime.compareTo(mDeviceI.getTime());
            long days=(qrTime.getTime()-mDeviceI.getTime().getTime())/(1000*3600*24);

            //if(days<-1)
            //   md5Result = 2;

            Log.i(TAG, "shijiancha: " + days);
            Log.i(TAG, "nowTime: " + mDeviceI.getTime().toString());
            Log.i(TAG, "qrTime: " + qrTime.toString());

            switch (md5Result) {
                case 1:
                    userName = userInfo1.substring(userInfo1.indexOf(" ") + 1, userInfo1.indexOf(" ", userInfo1.indexOf(" ") + 1));
                    u82uc uu = new u82uc();
                    byte[] textBuf = uu.utf8ToUnicode(userName);
                    //将UTF8编码文字转换为UNICODE编码，通过串口输出语音
                    try {
                        writeUartData(mDevice, textBuf);
                        Log.d(TAG, "ojbk!!!!" + textBuf[0] + textBuf[1] + textBuf[2] + textBuf[3] + userName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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


                    try {
                        Thread.currentThread().sleep(2000);//阻断2秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    checkTextEnd = null;
                    userInfo1 = null;
                    userInfo = null;
                    userInfo2 = null;
                    userName = null;
                    //继电器闭合
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

                case 2:
                        md5Result = 0;


            }
        }

    }

    private void stopUsbConnection() {
        try {
            if (serialDevice != null) {
                serialDevice.close();
            }

            if (connection != null) {
                connection.close();
            }
        } finally {
            serialDevice = null;
            connection = null;
        }
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }


}
