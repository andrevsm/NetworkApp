package br.com.tecnologia.positivo.networkinformation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.CellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 123;//any number
    TelephonyManager tm;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE,
    };
    private ListAdapter adapter;
    private HashMap<String, String> itemMap = new HashMap<>();
    private boolean onlyValidItems = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setUI();
        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_only_valid:
                onlyValidItems = true;
                loadData();
                return true;
            case R.id.menu_all:
                onlyValidItems = false;
                loadData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setUI() {
        updateTimeLabel();
        setListener();
        setRecycleView();
    }

    private void updateTimeLabel() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYY HH:mm:ss", Locale.getDefault());
        String updateText = "Ultima atualização: " + dateFormat.format(new Date());
        ((TextView) findViewById(R.id.tvi_update_date)).setText(updateText);
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
        itemMap.clear();
        String netTypeStr = getNetWorkType();

        itemMap.put("Network Type", netTypeStr);
        getCellInfo();
        setPhoneStateListener();
    }

    private void setPhoneStateListener() {
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                String signalStrJson = new Gson().toJson(signalStrength);
                Log.d("TESTE","JSON PhoneState: "+signalStrJson);
                String[] itemList = signalStrJson.replaceAll("[\"{}]", "").split(",");
                itemMap.put("====="," PhoneState ====");
                for (String anItemList : itemList) {
                    String[] split = anItemList.split(":");
                    String attributeName = split[0];
                    String attributeValueString = split[1];
                    attributeName = removePreFix(attributeName);
                    if (onlyValidItems) {
                        try {
                            int attributeValue = Integer.parseInt(attributeValueString);
                            Log.d("TESTE",attributeName+" - "+attributeValue+"="+isValidValue(attributeName, attributeValue));
                            if (isValidValue(attributeName, attributeValue)) {
                                itemMap.put(attributeName, attributeValueString);
                            }else Log.d("TESTE","Invalido Value: "+attributeName+":"+attributeValue);
                        } catch (Exception e) {
                            itemMap.put(attributeName, attributeValueString);
                        }
                    } else{
                        itemMap.put(attributeName, attributeValueString);
                    }
                }
                adapter.updateItems(itemMap);
                updateTimeLabel();
            }
        };
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public boolean isValidValue(String attributeName, int attributeValue) {
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
        int INVALID = Integer.MAX_VALUE;
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
            case "mWcdmaSignalStrength":
                return attributeValue != invalidWcdmaSignalStrength;
            case "mWcdmaRscpAsu":
                return attributeValue != invalidWcdmaRscpAsu;
            case "mLteRsrpBoost":
                return attributeValue != invalidLteRsrpBoost;
            case "LteRsrp":
            case "LteRsrq":
            case "LteRssnr":
            case "LteCqi":
            case "TdScdmaRscp":
            case "mGsmRssiQdbm":
            case "mWcdmaRscp":
            case "rssnr":
            case "mMnc":
            case "mCi":
            case "mMcc":
            case "ta":
            case "mTa":
            case "cqi":
                return attributeValue != INVALID;
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
        } else
            return s;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode== LOCATION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.i("TESTE", "Permission has been denied by user");
                requestPermission();
            } else {
                loadData();
            }
        }
    }
    private Boolean hasPermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,PERMISSIONS,LOCATION_REQUEST_CODE);
    }

    private void getCellInfo() {
        if (hasPermission()){
            List<CellInfo> cellInfoList = tm.getAllCellInfo();
            Log.d("TESTE","JSON CellInfo: "+new Gson().toJson(cellInfoList));
            itemMap.put("===="," CellInfo ====");
            for (CellInfo cellInfo : cellInfoList)
            {
                String cellInfoString = cellInfo.toString();
                String removedSpecialCharacter = cellInfoString.replaceAll("[\"{}]", "");
                String[] items = removedSpecialCharacter.split(" ");

                for (String item:items){
                    String suffix = removeSuffix(item, ":");
                    String[] attribute = suffix.split("=");
                    String attributeName = attribute[0];
                    if (attribute.length>1){
                        String attributeValueString = attribute[1];
                        String s = removePreFix(attributeName);
                        if (onlyValidItems){
                            try {
                                int attributeValue = Integer.parseInt(attributeValueString);
                                if (isValidValue(attributeName, attributeValue)){
                                    itemMap.put(s, attributeValueString);
                                }else Log.d("TESTE","Invalido Value: "+attributeName+":"+attributeValue);
                            }catch (Exception e){
                                itemMap.put(s, attributeValueString);
                            }
                        }else
                            itemMap.put(s, attributeValueString);
                    }
                }
            }
            adapter.updateItems(itemMap);
        }
        else
            requestPermission();
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
