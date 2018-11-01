package br.com.tecnologia.positivo.networkinformation;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int INVALID = Integer.MAX_VALUE;
    TelephonyManager tm;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE,
    };
    private ListAdapter adapter;
    private HashMap itemMap = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setUI();
        loadData();
    }

    private void setUI() {
        setListener();
        setRecycleView();
    }

    private void setRecycleView() {
        RecyclerView rviListItems = findViewById(R.id.rviListItems);
        adapter = new ListAdapter(new ArrayList<String>());
        rviListItems.setLayoutManager(new LinearLayoutManager(this));
        rviListItems.setHasFixedSize(true);
        rviListItems.addItemDecoration(new DividerItemDecoration(rviListItems.getContext(), DividerItemDecoration.VERTICAL));
        rviListItems.setAdapter(adapter);
    }

    private void setListener() {
        findViewById(R.id.btnRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadData();
            }
        });
    }

    private void loadData() {
        String NetTypeStr = getNetWorkType();
        Log.d("TESTE", "NetTypeStr: " + NetTypeStr);
        if (hasPermissions(PERMISSIONS)) {
            getCellInfo();
        } else
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);

        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                String signalStrJson = new Gson().toJson(signalStrength);
                String[] itemList = signalStrJson.replaceAll("[\"{}]", "").split(",");
                for (String anItemList : itemList) {
                    String[] split = anItemList.split(":");
                    String attributeName = split[0];
                    String attributeValueString = split[1];
                    try {
                        int attributeValue = Integer.parseInt(attributeValueString);
                        if (isValidValue(attributeName, attributeValue)){
                            attributeName = removePreFix(attributeName);
                            itemMap.put(attributeName, attributeValueString);
                        }
                        Log.d("TESTE","INT: "+attributeValueString);
                    }catch (Exception e){
                        Log.d("TESTE","STRING: "+attributeValueString);
                        attributeName = removePreFix(attributeName);
                        itemMap.put(attributeName, attributeValueString);
                    }

                }
                adapter.updateItems(itemMap);
            }
        };
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

    }

    public  boolean isValidValue(String attributeName,int attributeValue){
        int invalidGsmSignalStrength = 99;
        int invalidGsmBitErrorRate = -1;
        int invalidCdmaDbm = -1;
        int invalidCdmaEcio = -1;
        int invalidEvdoDbm = -1;
        int invalidEvdoEcio = -1;
        int invalidEvdoSnr = -1;
        int invalidLteSignalStrength = 99;
        int invalidWcdmaSignalStrength = 99;
        int invalidWcdmaRscpAsu = 255;
        int invalidLteRsrpBoost = 0;
        switch (attributeName) {
            case "mGsmSignalStrength":
                return attributeValue != invalidGsmSignalStrength;
            case "mGsmBitErrorRate":
                return attributeValue != invalidGsmBitErrorRate;
            case "mCdmaDbm":
                return attributeValue != invalidCdmaDbm;
            case "mCdmaEcio":
                return attributeValue != invalidCdmaEcio;
            case "mEvdoDbm":
                return attributeValue != invalidEvdoDbm;
            case "mEvdoEcio":
                return attributeValue != invalidEvdoEcio;
            case "mEvdoSnr":
                return attributeValue != invalidEvdoSnr;
            case "mLteSignalStrength":
                return attributeValue != invalidLteSignalStrength;
            case "mLteRsrp":
            case "mLteRsrq":
            case "mLteRssnr":
            case "mLteCqi":
            case "mTdScdmaRscp":
            case "mWcdmaRscp":
                return attributeValue != INVALID;
            case "mWcdmaSignalStrength":
                return attributeValue != invalidWcdmaSignalStrength;
            case "mWcdmaRscpAsu":
                return attributeValue != invalidWcdmaRscpAsu;
            case "mLteRsrpBoost":
                return attributeValue != invalidLteRsrpBoost;
            default:
                return true;
        }
    }

    private String removePreFix(String s) {
        boolean startsWithM = s.startsWith("m");
        boolean startsWithIs = s.startsWith("is");
        if (startsWithM)
            return s.substring(1);
        else if (startsWithIs) {
            return s.substring(2);
        }else
            return s;
    }
    private void getCellInfo() {
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        for (CellInfo cellInfo : cellInfoList)
        {
            String cellInfoString = cellInfo.toString();
            String removedSpecialCharacter = cellInfoString.replaceAll("[\"{}]", "");
            String[] items = removedSpecialCharacter.split(" ");

            for (String item:items){
                String suffix = removeSuffix(item, ":");
                String[] attribute = suffix.split("=");
                String attributeName = attribute[0];
                String attributeValue = attribute[1];

                String s = removePreFix(attributeName);
                if (attribute.length>1){
                    itemMap.put(s, attributeValue);
                }else{
                    itemMap.put(s,"");
                }
            }
        }
        adapter.updateItems(itemMap);
    }
    public String removeSuffix(final String s, final String suffix){
        if (s != null && s.contains(suffix)) {
            String[] split = s.split(suffix);
            if (split.length>1)
                return split[1];
            else return split[0].replace(":","");
        }
        return s;
    }



    public boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getNetWorkType() {

        switch(tm.getNetworkType()){
            case 0: return getString(R.string.unknown);
            case 1: return "GPRS";
            case 2: return "EDGE";
            case 3: return "UMTS";
            case 4: return "CDMA";
            case 5: return "EVDO_0";
            case 6: return "EVDO_A";
            case 7: return "1xRTT";
            case 8: return "HSDPA";
            case 9: return "HSUPA";
            case 10: return "HSPA";
            case 11: return "iDen";
            case 12: return "EVDO_B";
            case 13: return "LTE";
            case 14: return "eHRPD";
            case 15: return "HSPA+";
            default:
                return getString(R.string.unknown);
        }
    }



}
