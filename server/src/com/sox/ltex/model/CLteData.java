/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import com.ithows.util.DateTimeUtils;
import java.io.PrintStream;
import java.util.ArrayList;

import com.ithows.util.UtilJSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 *  Eid, Time, ,M System(7-LTE, 20-WLAN), 0, Count , parameter count, Cell type(0-serving,2-Listed), band, channel, pci, rssi, rsrp, rsrq, timing, Pathloss,Srxlev, 
 *  CELLMEAS,15:27:16.217,,7,0,1,10,0,70003,1550,318,-43.0,-70.0,-5.0,,93.0,
 *  CELLMEAS,14:35:51.792,,7,0,2,11,0,70003,1550,318,-44.0,-73.1,-9.7,173229,97.0,54.0/,,2,70003,1550,380,-61.1,-90.3,-17.1,170556,,37.0,
 *  CELLMEAS,14:35:59.077,,7,0,3,11,0,70003,1550,318,-48.4,-74.7,-6.3,173235,92.0,,,2,70003,1550,380,-51.9,-90.1,-18.1,170550,,,,2,70003,1550,321,-53.9,-94.1,-20.2,,,,
 */
public class CLteData {
    public int MCC = 450;    // 국가코드
    public int MNC = 0;      // 8-KT, 5-SKT, 6,7-LGU

    public String time = "";
    public String cellid = "0";
    public int sigType = 1;
    public int cellType = 0;    // 0 : lte serving  2 : lte neighboring  3: nr neighboring  4 : nr serving
    public int band = 0;
    public int channel = 0;   // earfcn  arfcn
    public int pci = 0;
    public double rssi = 0;
    public double rsrp = 0;
    public double rsrq = 0;
    public int timing = 0;
    public double pathloss = 0;
    public double srxlev = 0;

    public long cid = 0;
    public long ECI = 0;
    public long eNBId = 0;
    public long SectorId = 0;

    public int nrflag = 0;  // 0 : lte, 1: 5g
    public int sinr = 0;  // sinr
    public int nr_dl_bandwidth = 0;  // nr_dl_bandwidth



    public CLteData(int mcc, int mnc){
        this.MCC = mcc;
        this.MNC = mnc;
    }

    public void print(PrintStream out){
        out.println("   time : " + time );
        out.println("   cellType : " + cellType );
        out.println("   nrflag : " + nrflag );
        out.println("   band : " + band );
        out.println("   channel : " + channel );
        out.println("   pci : " + pci );
        out.println("   cid : " + cid );
        out.println("   ECI : " + ECI );
        out.println("   eNBId : " + eNBId );
        out.println("   SectorId : " + SectorId );
        out.println("   sinr : " + sinr );
        out.println("   nr_dl_bandwidth : " + nr_dl_bandwidth );
        out.println(String.format("   rssi : %.7f", rssi));
        out.println(String.format("   rsrq : %.7f", rsrq));
        out.println(String.format("   rsrp : %.7f", rsrp));
        out.println("   timing : " + timing );
        out.println(String.format("   pathloss : %.2f", pathloss));
        out.println(String.format("   srxlev : %.2f", srxlev));
    }

    public JSONObject getJSON(){
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("cellType", cellType);
            obj.put("nrflag", nrflag);
            obj.put("band", band);
            obj.put("channel", channel);
            obj.put("pci", pci);
            obj.put("cid", cid);
            obj.put("ECI", ECI);
            obj.put("eNBId", eNBId);
            obj.put("SectorId", SectorId);
            obj.put("sinr", sinr);
            obj.put("nr_dl_bandwidth", nr_dl_bandwidth);
            obj.put("rssi", rssi);
            obj.put("rsrq", rsrq);
            obj.put("rsrp", rsrp);
            obj.put("timing", timing);
            obj.put("pathloss", pathloss);
            obj.put("srxlev", srxlev);

        } catch (JSONException ex) {
            System.out.println(ex.getLocalizedMessage());
        }

        return obj;
    }

    /**
     * 앱에서 받은 데이터를 CLteObject로 변환
     * @param jArr
     * @return
     */
    public static ArrayList<CLteData> convertFromJSONArray(JSONArray jArr){
        ArrayList<CLteData> list = new ArrayList<CLteData>();

        if(jArr == null){
            return list;
        }

        for(int i=0 ; i< jArr.length() ; i++){
            try {
                JSONArray arr = jArr.getJSONArray(i);
                for(int j=0; j < arr.length() ; j++){
                    CLteData lteObj = new CLteData(0,0);
                    JSONObject jObj = arr.getJSONObject(j);

                    try{
//                        lteObj.cellType = (j==0) ? 0 : 2;   // 첫번째 것이 서빙셀

                        long cellid = 0;
                        if(jObj.has("nci")){
                            cellid = UtilJSON.getJsonElementLong(jObj,"nci", 0);
                            lteObj.cellid = "" + cellid;
                            lteObj.cellType = (jObj.getInt("nci") == 0) ? 3 : 4;
                        }else{
                            cellid = UtilJSON.getJsonElementLong(jObj,"cid" ,0);
                            lteObj.cellid = "" + cellid;
                            lteObj.cellType = (jObj.getInt("cid") == 0) ? 2 : 0;
                        }


                        lteObj.MCC = UtilJSON.getJsonElementInt(jObj, "mcc", 450);  // !jObj.getString("mcc").equals("") ? Integer.parseInt(jObj.getString("mcc")) : 0;
                        lteObj.MNC = UtilJSON.getJsonElementInt(jObj, "mnc", 0); //!jObj.getString("mnc").equals("") ? Integer.parseInt(jObj.getString("mnc")) : 0;
                        lteObj.channel = UtilJSON.getJsonElementInt(jObj, "band", 0); // Integer.parseInt(jObj.getString("band"));

                        lteObj.band += 70000;

                        lteObj.pci = UtilJSON.getJsonElementInt(jObj, "pci", 0);  //jObj.getInt("pci");

                        lteObj.setECI(lteObj.cellType == 0 || lteObj.cellType == 4 ? cellid : 0);   // @@ cellID  2025 5G 관련 (NR 반영)
                        lteObj.rsrq =  UtilJSON.getJsonElementDouble(jObj, ("rsrq"), -120.0);
                        lteObj.rsrp = UtilJSON.getJsonElementDouble(jObj, ("rsrp"), -120.0);;
                        lteObj.rssi = UtilJSON.getJsonElementDouble(jObj, ("rssi"), -100.0);;

                        lteObj.time = DateTimeUtils.convertTimestampToDate(jObj.getLong("time"));
                        list.add(lteObj);

                    }catch(Exception ex){
                        System.out.println(ex.getLocalizedMessage());
                    }


                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }


        return list;
    }



    public void setECI(long ECIInt){
        CLteData.setECI(ECIInt, this);
    }

    private static void setECI(long ECIInt, CLteData obj){

        if(ECIInt == -1){
            obj.ECI = -1;
            obj.eNBId = -1;
            obj.SectorId = -1;
            return ;
        }else if(ECIInt == 0){
            obj.ECI = 0;
            obj.eNBId = 0;
            obj.SectorId = 0;
            return ;
        }

        String ECIStr = Long.toHexString(ECIInt);
        String eNB_ID = ECIStr.substring(0, ECIStr.length()-2);
        int eNBInt = eNB_ID.equals("") ? 0 : Integer.parseInt(eNB_ID, 16);
        String Sector_ID = ECIStr.substring(ECIStr.length()-2, ECIStr.length());
        int SectorInt = Sector_ID.equals("") ? 0 : Integer.parseInt(Sector_ID, 16);

        obj.ECI = ECIInt;
        obj.eNBId = eNBInt;
        obj.SectorId = SectorInt;
    }

}
