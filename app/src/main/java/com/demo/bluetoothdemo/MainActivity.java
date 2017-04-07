package com.demo.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.service.GpPrintService;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * 有网：
 * 衣物名称  订单号  数量  备注 第一行固定四到五个标签  内容由网络获取
 * 。。。				       第二—n行  数据填充     有一个行数判断  动态添加  应该就是个for循环
 * 二维码/条形码   简单
 * 万一没有网：
 * 固定格式：文本输入 判断回车
 */
public class MainActivity extends Activity {
    private TextView tv_saomiao,tv_print;
    private String[] datas = {"蓝牙1", "蓝牙2", "蓝牙3", "蓝牙4", "蓝牙5", "蓝牙6", "蓝牙7", "蓝牙8", "蓝牙9", "蓝牙10",};
    private ListView listView_blue;
    private ListView buletooth_list;
    private PopupWindow popupWindow;
    private View popupView;

    private GpCom.ERROR_CODE r;
    private BuleToothListAdapter arrayAdapter;
    private List<String> nameData = new ArrayList<>();
    private List<String> adressData = new ArrayList<>();
    private List<BluetoothDevice> deviceData = new ArrayList<>();
    private int loction = -1;


    private GpService mGpService;
    private BluetoothAdapter bluetoothAdapter;
    //是否已经链接的标志
    private boolean flag;
    //已经链接的设备地址
    private String address;
    private PrinterServiceConnection conn = null;

    //打印
    private static final int MAIN_QUERY_PRINTER_STATUS = 0xfe;
    private static final int REQUEST_PRINT_RECEIPT = 0xfc;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case ResultCode.STARTSERVER:
                    connect((int) msg.obj);
                    break;
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_print = (TextView) findViewById(R.id.tv_print);
        tv_saomiao = (TextView) findViewById(R.id.tv_saomiao);
        connection();


        // 注册实时状态查询广播
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_DEVICE_REAL_STATUS));
        /**
         * 票据模式下，可注册该广播，在需要打印内容的最后加入addQueryPrinterStatus()，在打印完成后会接收到
         * action为GpCom.ACTION_DEVICE_STATUS的广播，特别用于连续打印，
         * 可参照该sample中的sendReceiptWithResponse方法与广播中的处理
         **/
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_RECEIPT_RESPONSE));
        /**
         * 标签模式下，可注册该广播，在需要打印内容的最后加入addQueryPrinterStatus(RESPONSE_MODE mode)
         * ，在打印完成后会接收到，action为GpCom.ACTION_LABEL_RESPONSE的广播，特别用于连续打印，
         * 可参照该sample中的sendLabelWithResponse方法与广播中的处理
         **/
        registerReceiver(mBroadcastReceiver, new IntentFilter(GpCom.ACTION_LABEL_RESPONSE));



        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /**
         * 点击扫描后
         * 1.判断蓝牙是否开启
         * 2.搜索蓝牙
         * 3.在popupwindow listview展示
         * 4.点击选择对应的蓝牙设备，点击后进行连接，成功则消失
         * 5.进入打印
         */

        tv_saomiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                if (mGpService == null) {
//                    Log.i("Blue","001");
//                    return;
//                }
                if (bluetoothAdapter == null) {
                    Log.i("Blue","002");
                    Toast.makeText(MainActivity.this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i("Blue","003");
                    if (!bluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        // 设置蓝牙可见性，最多300秒   
                        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
                        startActivityForResult(intent, ResultCode.BULETOOTH);
                    } else {
                        //如果蓝牙已经打开，进行操做
//                        nameData.clear();
//                        adressData.clear();
//                        deviceData.clear();
                        tv_saomiao.setText("正在扫描...");
                        searchBuleTooth();//扫描设备


                        //构造布局
                        popupView = MainActivity.this.getLayoutInflater().inflate(R.layout.popupwindow, null);
                        //添加数据
                        buletooth_list = (ListView) popupView.findViewById(R.id.listView_blue);
                        arrayAdapter = new BuleToothListAdapter(MainActivity.this, nameData, adressData);
                        //buletooth_list.setAdapter(new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,datas));
                        buletooth_list.setAdapter(arrayAdapter);
                        buletooth_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Toast.makeText(MainActivity.this,"点我了"+position,Toast.LENGTH_SHORT).show();
                                //进行连接蓝牙
                                tv_saomiao.setText("正在配对...");
                                bluetoothAdapter.cancelDiscovery();//停止扫描
                                BluetoothDevice device = deviceData.get(position);
                                int buleState = device.getBondState();
                                loction = position;
                                switch(buleState){
                                    // 未配对      
                                    case BluetoothDevice.BOND_NONE:
                                        try {
                                            Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                                            createBondMethod.invoke(device);
                                            tv_saomiao.setText("扫描蓝牙设备");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        break;
                                    //已配对
                                    case BluetoothDevice.BOND_BONDED :
                                        ProgressDialogUtil.showLoading(MainActivity.this,"获取服务...");
                                        tv_saomiao.setText("配对成功");
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                int rel = 0;
                                                try {//使用端口1，4代表模式为蓝牙模式，蓝牙地址，最后默认为0
                                                    rel = mGpService.openPort(1,4,adressData.get(loction), 0);
                                                } catch (RemoteException e) {
                                                    e.printStackTrace();
                                                }
                                                r = GpCom.ERROR_CODE.values()[rel];
                                                int flag = 0;
                                                while(r == GpCom.ERROR_CODE.SUCCESS && flag<=5){
                                                    try {//使用端口1，4代表模式为蓝牙模式，蓝牙地址，最后默认为0
                                                        Thread.sleep(1000);
                                                        rel = mGpService.openPort(1,4,adressData.get(loction), 0);
                                                        r = GpCom.ERROR_CODE.values()[rel];
                                                        flag++;
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                Message msg = new Message();
                                                msg.what = ResultCode.STARTSERVER;
                                                msg.obj = flag;
                                                handler.sendMessage(msg);
                                            }
                                        }).start();
                                        break;
                                }
                            }
                        });
                        //创建popupwindow对象 指定宽高
                        WindowManager wm = MainActivity.this.getWindowManager();
                        int width = wm.getDefaultDisplay().getWidth();
                        popupWindow = new PopupWindow(popupView, width, 600);
                        //动画
                        popupWindow.setAnimationStyle(R.style.popup_window_anim);
                        //背景    backgroundDrawable!=null的情况下设置 否则返回按钮或点击其他区域都会没有反应
                        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFFFFF")));
                        //popupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));//改成透明背景
                        //获取焦点
                        popupWindow.setFocusable(true);
                        //设置可以触摸弹出框以外的区域
                        popupWindow.setOutsideTouchable(true);
                        //更新popupwindow状态
                        popupWindow.update();
                        //显示方式及显示位置
                        popupWindow.showAsDropDown(tv_saomiao, 0, 0);

                    }
                }


            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


        tv_print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //判断蓝牙是否开启以及是否连接蓝牙
                if (mGpService == null) {
                    Toast.makeText(MainActivity.this,"服务正在开启",Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        int type = mGpService.getPrinterCommandType(1);
                        if (type == GpCom.ESC_COMMAND) {
                            mGpService.queryPrinterStatus(1, 1000, REQUEST_PRINT_RECEIPT);
                        } else {
                            Toast.makeText(MainActivity.this, "请连接蓝牙打印机", Toast.LENGTH_SHORT).show();
                        }
                    } catch (RemoteException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
    }
    private Thread thread = new Thread(){
        @Override
        public void run() {
            super.run();

        }
    };
    /**
     * 连接
     */
    private void connect(int flag) {
//        int rel = 0;
//        try {//使用端口1，4代表模式为蓝牙模式，蓝牙地址，最后默认为0
//            rel = mGpService.openPort(1,4,adressData.get(loction), 0);
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        r = GpCom.ERROR_CODE.values()[rel];
        if (flag <= 5) {
            if (r == GpCom.ERROR_CODE.DEVICE_ALREADY_OPEN) {
                //开启成功
                ProgressDialogUtil.dismiss(MainActivity.this);
                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                //0407  alread_tv.setVisibility(View.VISIBLE);
                //0407 alread_rl.setVisibility(View.VISIBLE);
                String name = nameData.get(loction);
                String address = adressData.get(loction);
                //0407 alread_name.setText(name);
                //0407 alread_id.setText(address);
                SharedPreferencesUtils.setDevice(1, name, address);//保存数据
                //连接成功后popupwindow消失
                popupWindow.dismiss();
                tv_saomiao.setText("已连接蓝牙设备:"+name+address);
// 0407               nameData.remove(loction);
//  0407              adressData.remove(loction);
//  0407              //刷新列表
//  0407              arrayAdapter.setData(nameData, adressData);
            } else {
                Toast.makeText(MainActivity.this, GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } else {
            ProgressDialogUtil.dismiss(MainActivity.this);
            Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 扫描设备
     */
    private void searchBuleTooth() {
        // 设置广播信息过滤   
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // 注册广播接收器，接收并处理搜索结果   
        registerReceiver(receiver, intentFilter);
        // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去   
        bluetoothAdapter.startDiscovery();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean flag = false;
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (nameData.size() == 0) {
                    adressData.add(device.getAddress());
                    deviceData.add(device);
                    if (device.getName() == null) {
                        nameData.add("无名称");
                    } else {
                        if (device.getName().equals("")) {
                            nameData.add("无名称");
                        } else {
                            nameData.add(device.getName());
                        }
                    }
                } else {
                    for (int i = 0; i < nameData.size(); i++) {
                        if (adressData.get(i).equals(device.getAddress())) {
                            flag = true;
                        }
                    }
                    if (flag) {
                        flag = false;
                    } else {
                        adressData.add(device.getAddress());
                        deviceData.add(device);
                        if (device.getName() == null) {
                            nameData.add("无名称");
                        } else {
                            if (device.getName().equals("")) {
                                nameData.add("无名称");
                            } else {
                                nameData.add(device.getName());
                            }
                        }
                    }
                }
                //如果有已经链接的设备，则去除设备列表中显示
                if (flag) {
                    for (int i = 0; i < adressData.size(); i++) {
                        if (adressData.get(i).equals(address)) {
                            adressData.remove(i);
                        }
                    }
                }
                //刷新列表
                arrayAdapter.setData(nameData, adressData);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int connectState = device.getBondState();
                switch (connectState) {
                    case BluetoothDevice.BOND_NONE:
                        break;
                    case BluetoothDevice.BOND_BONDING:
//                        ProgressDialogUtil.dismiss(BuleToothActivity.this);
//                        ProgressDialogUtil.showLoading(BuleToothActivity.this,"配对中...");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        ProgressDialogUtil.dismiss(MainActivity.this);
                        ProgressDialogUtil.showLoading(MainActivity.this, "获取服务...");
                        // 连接
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int rel = 0;
                                try {//使用端口1，4代表模式为蓝牙模式，蓝牙地址，最后默认为0
                                    rel = mGpService.openPort(1, 4, adressData.get(loction), 0);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                r = GpCom.ERROR_CODE.values()[rel];
                                int flag = 0;
                                while (r == GpCom.ERROR_CODE.SUCCESS && flag <= 5) {
                                    try {//使用端口1，4代表模式为蓝牙模式，蓝牙地址，最后默认为0
                                        Thread.sleep(1000);
                                        rel = mGpService.openPort(1, 4, adressData.get(loction), 0);
                                        r = GpCom.ERROR_CODE.values()[rel];
                                        flag++;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                Message msg = new Message();
                                msg.what = ResultCode.STARTSERVER;
                                msg.obj = flag;
                                handler.sendMessage(msg);
                            }
                        }).start();
                        break;
                }
            }
        }
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
    //连接打印机
    private void connection() {
        conn = new PrinterServiceConnection();
        Intent intent = new Intent(this, GpPrintService.class);
        this.bindService(intent, conn, Context.BIND_AUTO_CREATE); // bindService
    }
    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGpService = null;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = com.gprinter.aidl.GpService.Stub.asInterface(service);
        }
    }


    //--------打印是以下的代码
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("TAG", action);
            // GpCom.ACTION_DEVICE_REAL_STATUS 为广播的IntentFilter
            if (action.equals(GpCom.ACTION_DEVICE_REAL_STATUS)) {
                // 业务逻辑的请求码，对应哪里查询做什么操作
                int requestCode = intent.getIntExtra(GpCom.EXTRA_PRINTER_REQUEST_CODE, -1);
                // 判断请求码，是则进行业务操作
                if (requestCode == MAIN_QUERY_PRINTER_STATUS) {

                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    String str;
                    if (status == GpCom.STATE_NO_ERR) {
                        str = "打印机正常";
                    } else {
                        str = "打印机 ";
                        if ((byte) (status & GpCom.STATE_OFFLINE) > 0) {
                            str += "脱机";
                        }
                        if ((byte) (status & GpCom.STATE_PAPER_ERR) > 0) {
                            str += "缺纸";
                        }
                        if ((byte) (status & GpCom.STATE_COVER_OPEN) > 0) {
                            str += "打印机开盖";
                        }
                        if ((byte) (status & GpCom.STATE_ERR_OCCURS) > 0) {
                            str += "打印机出错";
                        }
                        if ((byte) (status & GpCom.STATE_TIMES_OUT) > 0) {
                            str += "查询超时";
                        }
                    }
                    Toast.makeText(getApplicationContext(), "打印机：" + 1 + " 状态：" + str, Toast.LENGTH_SHORT)
                            .show();
                } else if (requestCode == REQUEST_PRINT_RECEIPT) {
                    int status = intent.getIntExtra(GpCom.EXTRA_PRINTER_REAL_STATUS, 16);
                    if (status == GpCom.STATE_NO_ERR) {
                        sendReceipt();
                    } else {
                      Toast.makeText(MainActivity.this, "请先连接蓝牙打印机", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        }
    };
    private void sendReceipt() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        //--->
//        esc.addPrintAndLineFeed();
////        esc.addPrintAndFeedLines((byte) 3);
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);// 设置打印居中
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);// 设置为倍高倍宽
//        esc.addText("商家订单\n"); // 打印文字
//        esc.addPrintAndLineFeed();
//
//		/* 打印文字 */
//        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);// 取消倍高倍宽
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);// 设置打印左对齐
//        esc.addText("商家\n"); // 打印文字
//
////        esc.addText("Welcome to use SMARNET printer!\n"); // 打印文字
////		/* 打印繁体中文 需要打印机支持繁体字库 */
////        String message = "佳博智匯票據打印機\n";
////        // esc.addText(message,"BIG5");
////        esc.addText(message, "GB2312");
//        esc.addPrintAndLineFeed();
//<---

		/* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("衣物名称");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("订单号");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("件数");
        esc.addPrintAndLineFeed();
        esc.addText("上衣");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("170406");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("1000000000");
        esc.addPrintAndLineFeed();
        //esc.addPrintAndLineFeed();//这个是换行的 加一个就有一个空行

//        esc.addText("果粒橙300ml");
//        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
//        esc.addSetAbsolutePrintPosition((short) 6);
//        esc.addText("3545456");
//        esc.addSetAbsolutePrintPosition((short) 10);
//        esc.addText("正常");
//        esc.addPrintAndLineFeed();
//		/* 打印图片 */
//        esc.addText("Print bitmap!\n"); // 打印文字
//        Bitmap b = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//        esc.addRastBitImage(b, 30, 0); // 打印图片（参数 bitmap width height）
//        esc.addPrintAndLineFeed();
        esc.addPrintAndLineFeed();
//		/* 打印一维条码 */
//        esc.addText("Print code128\n"); // 打印文字
//        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);//
//        // 设置条码可识别字符位置在条码下方
//        esc.addSetBarcodeHeight((byte) 60); // 设置条码高度为60点
//        esc.addSetBarcodeWidth((byte) 1); // 设置条码单元宽度为1
//        esc.addCODE128(esc.genCodeB("SMARNET")); // 打印Code128码
//        esc.addPrintAndLineFeed();
		/*
		 * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
		 */
        //--->
//        esc.addText("商家二维码\n"); // 打印文字
//        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31); // 设置纠错等级
//        esc.addSelectSizeOfModuleForQRCode((byte) 6);// 设置qrcode模块大小
//        esc.addStoreQRCodeData("www.baidu.com");// 设置qrcode内容
//        esc.addPrintQRCode();// 打印QRCode
//        esc.addPrintAndLineFeed();
        //<---

		/* 打印文字 */
//
        // 开钱箱
     //   esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
//        esc.addPrintAndFeedLines((byte) 8);
        esc.addPrintAndLineFeed();

        Vector<Byte> datas = esc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String sss = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rs;
        try {
            rs = mGpService.sendEscCommand(1, sss);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rs];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}







