package com.example.administrator.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
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
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Date;


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

    private static final int USB_VENDOR_ID = 6790;
    private static final int USB_PRODUCT_ID = 29987;//ch340 vid pid
    private static final int USB_VENDOR_ID2 = 1027;
    private static final int USB_PRODUCT_ID2= 24577;//ft232 vid pid

    private UsbManager usbManager;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialDevice;
    private String buffer;
    private byte[] databyte;
    private int datacount = 0;


    private UsbSerialInterface.UsbReadCallback callback = new UsbSerialInterface.UsbReadCallback() {
        // USB转串口回调程序
        @Override
        public void onReceivedData(byte[] data) {
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
                while ((index = buffer.indexOf('\n')) != -1) {
                    datacount = 0;
                    final String dataStr = buffer.substring(0, index + 1).trim();
                    buffer = buffer.length() == index ? "" : buffer.substring(index + 1);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                // 对接收的数据进行处理
                                onSerialDataReceived(dataStr);
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

        // 程序启动时播报“程序启动”
        byte[] textBuf0 ={0x7a,0x0b,0x5e,(byte)0x8f,0x54,0x2f,0x52,(byte)0xa8};
        try {
            writeUartData(mDevice,textBuf0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 初始化DS3231时钟模块，第一次使用或没电时须打开注释重新设置时间
        //Date date;
        try {
            mDeviceI = new Ds3231(I2C_DEVICE_NAME);
            Log.d(TAG, "isTimekeepingDataValid = " + mDeviceI.isTimekeepingDataValid());
            Log.d(TAG, "isOscillatorEnabled = " + mDeviceI.isOscillatorEnabled());

            //Calendar calendar = Calendar.getInstance();
            //calendar.set(2018, Calendar.APRIL, 12,8,17,00);

            //date = calendar.getTime();

            //Log.d(TAG, "DateTime = " + date.toString());
            //mDeviceI.setTime(date);

            //noinspection ConstantConditions
            Log.d(TAG, "getTime = " + mDeviceI.getTime().toString());
            //mDeviceI.setTime(date.getTime());
            Log.d(TAG, "getTime = " + mDeviceI.getTime().toString());

            //mDeviceI.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while opening screen", e);
            throw new RuntimeException(e);
        }

    }
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "success2");
        Log.d(TAG, "aaaaaaaaaaaaaa!!!!"+md5Result);

        // 打开两个USB转串口
        startUsbConnection2();
        startUsbConnection();
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
                    startSerialConnection(device);
                    return;
                }
            }
        }
        Log.w(TAG, "Could not start USB connection - No devices found");
    }

    private void startSerialConnection(UsbDevice device) {
        //配置USB转串口，打开连接
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
        //从USB转串口接收数据后进行处理
        // Add whatever you want here
        Log.i(TAG, "Serial data received: " + data);
        if(data.length()>13) {
            //将二维码中逗号换成空格
            userInfo1 = data.replace(",", " ");
            MD5Util md5 = new MD5Util();
            //md5校验
            md5Result = md5.md5Check(userInfo1.substring(0, userInfo1.length() - 9), userInfo1.substring(userInfo1.length() - 8, userInfo1.length()));

            //将二维码信息以空格为分割分为数组
            String userInfoSp[] = userInfo1.split(" ");
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            if (userInfoSp.length>6) {
                String userTime = userInfoSp[3] + " " + userInfoSp[4];
                Date qrTime = format.parse(userTime);
                //将二维码中时间与现在时间比对得到时间差
                @SuppressWarnings("ConstantConditions") long days = (qrTime.getTime() - mDeviceI.getTime().getTime()) / (1000 * 3600 * 24);


                Log.i(TAG, "shijiancha: " + days);
                Log.i(TAG, "nowTime: " + mDeviceI.getTime().toString());
                Log.i(TAG, "qrTime: " + qrTime.toString());
            }

            switch (md5Result) {
                case 1:
                    /* 验证成功
                       将UTF8编码文字转换为UNICODE编码，通过串口输出语音 */
                    userName = userInfo1.substring(userInfo1.indexOf(" ") + 1, userInfo1.indexOf(" ", userInfo1.indexOf(" ") + 1));
                    byte[] textBuf = u82uc.utf8ToUnicode(userName);
                    try {
                        writeUartData(mDevice, textBuf);
                        Log.d(TAG, "ojbk!!!!" + textBuf[0] + textBuf[1] + textBuf[2] + textBuf[3] + userName);
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
                        Thread.sleep(3000);//阻断3秒
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


                case 2:
                    //验证失败
                    md5Result = 0;


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
        } finally {
            serialDevice = null;
            connection = null;
        }
    }

    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        //将两个byte数组拼接
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }


}
