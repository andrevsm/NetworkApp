package br.com.tecnologia.positivo.networkinformation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
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
    boolean btnStart;
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-YYY HH:mm:ss",
                Locale.getDefault());
        String updateText = "Ultima atualização: " + dateFormat.format(new Date());
        ((TextView) findViewById(R.id.tvi_update_date)).setText(updateText);
    }

    private void setRecycleView() {
        RecyclerView rviListItems = findViewById(R.id.rviListItems);
        adapter = new ListAdapter(new ArrayList<String>());
        rviListItems.setLayoutManager(new LinearLayoutManager(this));
        rviListItems.setHasFixedSize(true);

        rviListItems.addItemDecoration(new DividerItemDecoration(rviListItems.getContext(),
                DividerItemDecoration.VERTICAL));
        rviListItems.setAdapter(adapter);
    }

    private void setListener() {
        final TextView btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(btnStart == false){
                    btnStart = true;
                    btnRefresh.setText(R.string.stop);
                } else {
                    btnStart = false;
                    btnRefresh.setText(R.string.start);
                }

                if(btnStart) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (btnStart) {
                                try {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            loadData();
                                        }
                                    });
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();
                }
            }
        });
    }

    private void loadData() {
        itemMap.clear();
        String netTypeStr = getNetWorkType();
        String netTypeSwitch = getNetWorkTypeInformation();
        if(netTypeSwitch.equals("3G")) {
            itemMap.put("Network Type", netTypeStr);
            getWCDMAInfo();
        } else if (netTypeSwitch.equals("2G")) {
            itemMap.clear();
            itemMap.put("Network Type", netTypeStr);
            getGSMInfo();
        } else if (netTypeSwitch.equals("4G")) {
            itemMap.clear();
            itemMap.put("Network Type", netTypeStr);
            getLTEInfo();
        } else {
            itemMap.clear();
            itemMap.put("Network Type", netTypeStr);
            adapter.updateItems(itemMap);
        }
    }

    private void setPhoneStateListener() {
        final PhoneStateListener phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                String signalStrJson = new Gson().toJson(signalStrength);
                String[] itemList = signalStrJson.replaceAll("[\"{}]", "").split(",");
                Log.d("TESTE", "JSONPHONESTATE: " + new Gson().toJson(signalStrength));
                for (String anItemList : itemList) {
                    String[] split = anItemList.split(":");
                    String attributeName = split[0];
                    if (split.length>1){
                        String s = removePreFix(attributeName);
                        String attributeValueString = split[1];
//                        attributeValueString = removeSuffix(attributeValueString, "0");
                        if(isRequiredValue(attributeName)){
                            itemMap.put(s, attributeValueString);
                            adapter.updateItems(itemMap);
                        }
                    }
                }
            }
        };
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public boolean isValidValue(String attributeName, String attributeValue) {
        String invalidGsmSignalStrength = "99";
        String invalidGsmBitErrorRate = "-1";
        String invalidCdmaDbm = "-1";
        String invalidCdmaEcio = "-1";
        String invalidEvdoDbm = "-1";
        String invalidEvdoEcio = "-1";
        String invalidEvdoSnr = "-1";
        String invalidLteSignalStrength = "99";
        String invalidWcdmaSignalStrength = "99";
        String invalidWcdmaRscpAsu = "255";
        String invalidLteRsrpBoost = "0";
        if (attributeValue.equals(Integer.toString(Integer.MAX_VALUE))) {
            return false;
        }
        switch (attributeName) {
        case "mGsmSignalStrength":
            return !attributeValue.equals(invalidGsmSignalStrength);
        case "mGsmBitErrorRate":
            return !attributeValue.equals(invalidGsmBitErrorRate);
        case "mCdmaDbm":
            return !attributeValue.equals(invalidCdmaDbm);
        case "mCdmaEcio":
            return !attributeValue.equals(invalidCdmaEcio);
        case "mEvdoDbm":
            return !attributeValue.equals(invalidEvdoDbm);
        case "mEvdoEcio":
            return !attributeValue.equals(invalidEvdoEcio);
        case "mEvdoSnr":
            return !attributeValue.equals(invalidEvdoSnr);
        case "mLteSignalStrength":
            return !attributeValue.equals(invalidLteSignalStrength);
        case "mWcdmaSignalStrength":
            return !attributeValue.equals(invalidWcdmaSignalStrength);
        case "mWcdmaRscpAsu":
            return !attributeValue.equals(invalidWcdmaRscpAsu);
        case "mLteRsrpBoost":
            return !attributeValue.equals(invalidLteRsrpBoost);
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
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.i("TESTE", "Permission has been denied by user");
                requestPermission();
            } else {
                loadData();
            }
        }
    }

    private Boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_REQUEST_CODE);
    }

//    private void getCellInfo() {
//        if (hasPermission()) {
//            List<CellInfo> cellInfoList = tm.getAllCellInfo();
//            CellInfoWcdma wcdma = (CellInfoWcdma) tm.getAllCellInfo().get(0);
//            Log.d("TESTE", "WCDMA: " + wcdma);
//            Log.d("TESTE", "JSON CellInfo: " + new Gson().toJson(cellInfoList));
//            itemMap.put("====", " CellInfo ====");
//            for (CellInfo cellInfo : cellInfoList) {
//                String cellInfoString = cellInfo.toString();
//                String removedSpecialCharacter = cellInfoString.replaceAll("[\"{}]", "");
//                String[] items = removedSpecialCharacter.split(" ");
//
//                for (String item : items) {
//                    String suffix = removeSuffix(item, ":");
//                    String[] attribute = suffix.split("=");
//                    String attributeName = attribute[0];
//                    if (attribute.length > 1) {
//                        String attributeValueString = attribute[1];
//                        String s = removePreFix(attributeName);
//                        if (onlyValidItems) {
//                            try {
//                                int attributeValue = Integer.parseInt(attributeValueString);
//                                if (isValidValue(attributeName, attributeValue)) {
//                                    itemMap.put(s, attributeValueString);
//                                } else
//                                    Log.d("TESTE", "Invalido Value: " + attributeName + ":" + attributeValue);
//                            } catch (Exception e) {
//                                itemMap.put(s, attributeValueString);
//                            }
//                        } else
//                            itemMap.put(s, attributeValueString);
//                    }
//                }
//            }
//            adapter.updateItems(itemMap);
//        } else
//            requestPermission();
//    }

    public String removeSuffix(final String s, final String suffix) {
        if (s != null && s.contains(suffix)) {
            String[] split = s.split(suffix);
            if (split.length > 1)
                return split[1];
            else return split[0].replace(":", "");
        }
        return s;
    }

    private String getNetWorkType() {
        switch (tm.getNetworkType()) {
            case 0:
                return getString(R.string.unknown);
            case 1:
                return "GPRS";
            case 2:
                return "EDGE";
            case 3:
                return "UMTS";
            case 4:
                return "CDMA";
            case 5:
                return "EVDO_0";
            case 6:
                return "EVDO_A";
            case 7:
                return "1xRTT";
            case 8:
                return "HSDPA";
            case 9:
                return "HSUPA";
            case 10:
                return "HSPA";
            case 11:
                return "iDen";
            case 12:
                return "EVDO_B";
            case 13:
                return "LTE";
            case 14:
                return "eHRPD";
            case 15:
                return "HSPA+";
            default:
                return getString(R.string.unknown);
        }
    }

    private String getNetWorkTypeInformation() {
        switch (tm.getNetworkType()) {
            case 1:
            case 2:
                return "2G";
            case 3:
            case 8:
            case 9:
            case 10:
            case 15:
                return "3G";
            case 13:
                return "4G";
            default:
                return getString(R.string.unknown);
        }
    }

    private boolean isRequiredValue(String attributeName) {
        String type = getNetWorkTypeInformation();
        if(type.equals("3G")){
            if(attributeName.equals("mCdmaEcio") || attributeName.equals("mEvdoDbm")) {
                return true;
            }
        } else if(type.equals("2G")){

        } else if(type.equals("4G")){

        }

        return false;
    }

    private String setArfcn(int attributeValue) {
        String type = getNetWorkTypeInformation();
//        Log.d("TESTE", "JSONATTRVALUE: " + attributeValue);
        if(type.equals("3G")){
            if(attributeValue >= 10562 && attributeValue <= 10838) {
                return "2100";
            }
            if(attributeValue >= 4357 && attributeValue <= 4458) {
                return "850";
            }
            switch (attributeValue) {
                case 1007:
                case 1012:
                case 1032:
                case 1037:
                case 1062:
                case 1087:
                    return "850";
            }
        } else if(type.equals("2G")){
            if(attributeValue >= 128 && attributeValue <= 251) {
                return "GSM-850";
            }
            if(attributeValue >= 0 && attributeValue <= 124 || attributeValue >= 975 && attributeValue <= 1023) {
                return "EGSM-900";
            }
            if(attributeValue >= 512 && attributeValue <= 885) {
                return "DCS-1800";
            }
        } else if(type.equals("4G")){
            if(attributeValue >= 1200 && attributeValue <= 1949) {
                return "1800";
            }
            if(attributeValue >= 2750 && attributeValue <= 3449) {
                return "2600";
            }
            if(attributeValue >= 9210 && attributeValue <= 9659) {
                return "700";
            }
        }
        return "unknown";
    }

    private void getWCDMAInfo() {
        if (hasPermission()) {
            CellInfoWcdma wcdma = (CellInfoWcdma)tm.getAllCellInfo().get(0);
            CellSignalStrengthWcdma signalStrengthWcdma = wcdma.getCellSignalStrength();
//            Log.d("TESTE", "JSONWCDMACELLINFO: " + signalStrengthWcdma);
            int wcdmaDbm = signalStrengthWcdma.getDbm();
            String wcdmaString = wcdma.toString();
            String removedSpecialCharacter = wcdmaString.replaceAll("[\"{}]", "");
            String[] items = removedSpecialCharacter.split(" ");
            itemMap.put("Wcdma dBm", Integer.toString(wcdmaDbm));

            for (String item : items){
                String suffix = removeSuffix(item, ":");
                String[] attribute = suffix.split("=");

                if (suffix.contains("TimeStamp") ) {
                    continue;
                }
                String attributeName = attribute[0];

                if(attributeName.equals("ss")){
                    attributeName = "Signal Strength";
                }

                if(attributeName.equals("mUarfcn")){
                    int attValue = Integer.parseInt(attribute[1]);
                    String stringValue = setArfcn(attValue) + " Mhz";
                    attributeName = "UMTS Frequency";
                    itemMap.put(attributeName, stringValue);
                    if(stringValue.equals("2100 Mhz")) {
                        itemMap.put("Band", " 1");
                    } else if (stringValue.equals("850 Mhz")) {
                        itemMap.put("Band", " 5");
                    }
                    continue;
                }

                if (attribute.length>1){
                    String attributeValueString = attribute[1];
                    String s = removePreFix(attributeName);
                    if(isValidValue(attributeName, attributeValueString)){
                        itemMap.put(s, attributeValueString);
                    }
                }
            }
            adapter.updateItems(itemMap);
            ListAdapter.setNetworkType("WCDMA INFORMATION");
            setPhoneStateListener();
            updateTimeLabel();
        }
        else
            requestPermission();
    }

    private void getGSMInfo() {
        if (hasPermission()) {
            CellInfoGsm gsm = (CellInfoGsm)tm.getAllCellInfo().get(0);
//            Log.d("TESTE", "JSONGSM: " + gsm);
            CellSignalStrengthGsm signalStrengthGsm = gsm.getCellSignalStrength();
//            Log.d("TESTE", "JSONGSMSIGNAL: " + signalStrengthGsm);
            int gsmDbm = signalStrengthGsm.getDbm();
            String gsmString = gsm.toString();
            String removedSpecialCharacter = gsmString.replaceAll("[\"{}]", "");
            String[] items = removedSpecialCharacter.split(" ");
            itemMap.put("GSM dBm", Integer.toString(gsmDbm));

            for (String item : items){
                String suffix = removeSuffix(item, ":");
                String[] attribute = suffix.split("=");
                if (suffix.contains("TimeStamp") ) {
                    continue;
                }
                String attributeName = attribute[0];
                if(attributeName.equals("ss")){
                    attributeName = "Signal Strength";
                }

                if(attributeName.equals("mArfcn")){
                    int attValue = Integer.parseInt(attribute[1]);
                    String stringValue = setArfcn(attValue) + " Mhz";
//                    Log.d("TESTE", "GSMattValue: " + attValue);
                    attributeName = "GSM Frequency";
                    itemMap.put(attributeName, stringValue);
                    continue;
                }

                if (attribute.length>1){
                    String attributeValueString = attribute[1];
                    String s = removePreFix(attributeName);
                    if(isValidValue(attributeName, attributeValueString)){
                        itemMap.put(s, attributeValueString);
                    }
                }
            }

            adapter.updateItems(itemMap);
            ListAdapter.setNetworkType("GSM INFORMATION");
            setPhoneStateListener();
            updateTimeLabel();
        }
        else
            requestPermission();
    }

    private void getLTEInfo() {
        if (hasPermission()) {
            CellInfoLte lte = (CellInfoLte)tm.getAllCellInfo().get(0);
//            Log.d("TESTE", "JSONLTE: " + lte);
            CellSignalStrengthLte signalStrengthLte = lte.getCellSignalStrength();
            int lteDbm = signalStrengthLte.getDbm();
            String lteString = lte.toString();
            String removedSpecialCharacter = lteString.replaceAll("[\"{}]", "");
            String[] items = removedSpecialCharacter.split(" ");
            itemMap.put("LTE dBm", Integer.toString(lteDbm));

            for (String item : items){
                String suffix = removeSuffix(item, ":");
                String[] attribute = suffix.split("=");
                if (suffix.contains("TimeStamp") ) {
                    continue;
                }
                String attributeName = attribute[0];
                if(attributeName.equals("ss")){
                    attributeName = "Signal Strength";
                }

                if(attributeName.equals("mEarfcn")){
                    int attValue = Integer.parseInt(attribute[1]);
//                    Log.d("TESTE", "JSONVALUE: " + attValue);
                    String stringValue = setArfcn(attValue) + " Mhz";
                    attributeName = "LTE Frequency";
                    itemMap.put(attributeName, stringValue);
                    if(stringValue.equals("1800 Mhz")) {
                        itemMap.put("Band", " 3");
                    } else if (stringValue.equals("2600 Mhz")) {
                        itemMap.put("Band", " 7");
                    } else if (stringValue.equals("700 Mhz")) {
                        itemMap.put("Band", " 28");
                    }
                    continue;
                }

                if (attribute.length>1){
                    String attributeValueString = attribute[1];
                    String s = removePreFix(attributeName);
                    if(isValidValue(attributeName, attributeValueString)){
                        itemMap.put(s, attributeValueString);
                    }
                }
            }
            adapter.updateItems(itemMap);
            ListAdapter.setNetworkType("LTE INFORMATION");
            updateTimeLabel();
        }
        else
            requestPermission();
    }

}
