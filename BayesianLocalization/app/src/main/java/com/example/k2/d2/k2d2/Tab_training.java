package com.example.k2.d2.k2d2;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.example.k2.d2.k2d2.bayesianLocalization.training_Setup;



public class Tab_training extends Fragment implements View.OnClickListener {

    private WifiManager wifiManager;
    private TextView scan_results;
    private Button cellscan, training_done, read_jsonfile;

    public int cellNumber;
    ArrayList<gsonParser> allItems = new ArrayList<>();
    static HashMap<String,Float[][]> pmf_data = new HashMap<>();

    static Float[] prior ;
    static Float[] posterior ;
    static int numberOfLevels = 50;
    static int rowsize = 16; //  row size for training data
    static int columnsize = numberOfLevels; // column size for training data
    static int cellnum = rowsize;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_training_data, container, false);
        scan_results = (TextView) rootView.findViewById(R.id.scan_results); // view to display the results. This is temp for debug purpose
        scan_results.setMovementMethod(new ScrollingMovementMethod());

        cellscan = (Button) rootView.findViewById(R.id.cellscan); // button used for scanning in each cell.
        cellscan.setOnClickListener(this);

        training_done = (Button) rootView.findViewById(R.id.write_JSON); // button to indicate when the training is done and
        training_done.setOnClickListener(this);

        read_jsonfile = (Button) rootView.findViewById(R.id.read_JSON); // button to read the JSOn file and display.
        read_jsonfile.setOnClickListener(this);

        Spinner spinner = rootView.findViewById(R.id.spinner1);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity().getApplicationContext(), R.array.cells, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
//               cellNumber =  (String) parent.getItemAtPosition(position);
                cellNumber = position; // setting the position to global variable cell number.
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        return rootView;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cellscan:
                cellscan.setEnabled(false);
                wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                List<ScanResult> previousScanResults = wifiManager.getScanResults();
                Toast.makeText(getActivity().getApplicationContext(), "Scan Started Please standby", Toast.LENGTH_SHORT).show();
                for(int count = 1; count<=4; count++) {
                    scan_results.setText("\nScan "+ count +" started");
                    boolean scanstarted = wifiManager.startScan();
                    List<ScanResult> scanResults = wifiManager.getScanResults();
                    if(!previousScanResults.toString().equals(scanResults.toString())){
                        previousScanResults = scanResults;
                        Toast.makeText(getActivity().getApplicationContext(), "Scan "+ count +" over", Toast.LENGTH_SHORT).show();
                        scan_results.setText("Scan "+ count +" over");
                    for (ScanResult scanResult : scanResults) {
                        scan_results.setText(scan_results.getText() + "\n\tBSSID = "
                                + scanResult.BSSID + "\tlevel=" + scanResult.level);

                        if (validAP(scanResult.BSSID)) {
                            scan_results.setText(scan_results.getText() + "\nvalid");
                            int scanLevel = WifiManager.calculateSignalLevel(scanResult.level, numberOfLevels);
                            if (scanLevel > 25) {
                                allItems.add(new gsonParser(scanResult.BSSID, scanLevel, cellNumber));
                                }
                            }
                        }
                    }else
                        count--;

//
                }

//                for (gsonParser item : allItems) {
//                    scan_results.setText(scan_results.getText() + "\n\tBSSID = " + item.getBSSI() +
//                            "    level = " + item.getRSSi() + "   cellNumber = "+ item.getCellNumber());
//                }
                Toast.makeText(getActivity().getApplicationContext(), "Cell Scanned", Toast.LENGTH_SHORT).show();
                cellscan.setEnabled(true);
                break;
            case R.id.write_JSON:
                writeFile(0); // refer function for the passing of the parameter. Writes to a JSON file based on the argument.
                scan_results.setText("Json file written");
                break;
            case R.id.read_JSON:
                gsonParser[] items = readFile(0);  // refer function for passing the parameter. Reads a JSON file based on the argument.
                training_Setup(items); // Main function to update the tables.
                writeFile(1); // refer function for the passing of the parameter. Writes to a JSON file based on the argument.
                scan_results.setText("Offline tables are set. Localization can be done from now on.");
                break;

        }
    }

    public void writeFile(int i){
        try {
            FileOutputStream fos ;
            String json;
            if(i == 0) { //for writing the training data
                fos = getContext().openFileOutput("test.json", MODE_PRIVATE);
                json = new Gson().toJson(allItems);
            }
            else { //for writing the pmf hashmap with key -> BSSi , value -> pmf_tables.
                fos = getContext().openFileOutput("pmf_data.json", MODE_PRIVATE);
                json = new Gson().toJson(pmf_data);
            }
            fos.write(json.getBytes());
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
        Function to read JSON files.
        Input 0 reads the scan results
        Input !=0 reads the pmf data --> used for offline storage. This will be used for localization.
        So that we don't have to create/store tables every time for localization.
     */
    public gsonParser[] readFile(int i){
        gsonParser[] items = new gsonParser[0];
        try {
            FileInputStream fos1;
            int size;
            if (i == 0) {
                fos1 = getContext().openFileInput("test.json"); // scan results.
            }
            else{
                fos1 = getContext().openFileInput("pmf_data.json"); // offline tables
            }
            size = fos1.available();
            byte[] buffer = new byte[size];
            fos1.read(buffer);
            String text = new String(buffer);
            if(i ==0){
                items = new Gson().fromJson(text, gsonParser[].class);
//                textRssi.setText(text);
                return items;
            }
            else{
                Type type = new TypeToken<HashMap<String, Float[][]>>(){}.getType();
                pmf_data = new Gson().fromJson(text,type);
                return items;
            }


        }catch (IOException e){

        }
        return items;
    }

    public boolean validAP(String BSSID){

        String first_4B= BSSID.substring(0,5);
        String[] BSS_enb={"50:1c","24:01","54:4a","00:3a","2c:33","b4:e9"};

        for(String enable: BSS_enb)
        {
            if ( first_4B.compareTo( enable)==0) {

                return true;
            }
        }
        return false;
    }

}
