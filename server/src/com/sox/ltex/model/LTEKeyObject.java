/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sox.ltex.model;

import com.sox.ltex.util.LTEKeyParser;
import com.ithows.ResultMap;
import com.ithows.util.UtilString;
import java.util.ArrayList;

/**
 *
 * @author mailt
 */
public class LTEKeyObject {
    public String telecomName = "";
    public int count = 0;   // 키의 수
    public String pciList = "";
    public String channelList = "";
    public String celltypeList = "";
    public String keyList = "";     // LTE 키 리스트

    public String lteMatchKey = "";    // 매칭된 LTE 키
    public String wifiMatchKey = "";    // 매칭된 WiFi 키

    // @@ 웹 리스트를 위한 멤버
    // 쿼리 매칭한 결과를 담음 --> LTEMatchDAO.selectMatchListCorp()
    public ArrayList<ResultMap> matchList = new ArrayList<ResultMap>();   // WiFi 매칭리스트
    
    public LTEKeyObject(){
        
    }
    
    public LTEKeyObject(String telecom, String keyString){
        setData(telecom, keyString);
    }
    
    
    // 이게 쓰임
    public LTEKeyObject(String telecom, ArrayList<ResultMap> list){
        setData(telecom, list);
    }
    
    private void setData(String telecom, String keyString){
        this.telecomName = telecom;
        String[] part = LTEKeyParser.departKey(keyString);
        pciList = part[0];
        channelList = part[1];
        celltypeList = part[2];
        keyList = keyString;
        count = UtilString.countElement(keyString, ",");
    }

    private void setData(String telecom, ArrayList<ResultMap> list){
        this.telecomName = telecom;
        //  = list;
        
        String keyString = "";
        
        if(list == null || list.size() == 0){
            return;
        }
        
        // @@ 더 필요한 부수적인 데이터는 여기에서 만든다.
        for(ResultMap map : list){
            if(map != null){
                keyString = map.getString(this.telecomName);
                keyString = UtilString.removeNullCsv(keyString, ",");
                
                if(keyString == null || keyString.equals("null") || keyString.isEmpty() ){
                    map.put(telecomName, "");
                    map.put("pci", "");
                    map.put("channel", "");
                    map.put("celltype", "");
                    map.put("ltekeycount", 0);
                    continue;
                }
            }

            String[] part = LTEKeyParser.departKey(keyString);
            map.put("pci", part[0]);
            map.put("channel", part[1]);
            map.put("celltype", part[2]);
            int cnt = UtilString.countElement(keyString, ",");            
            map.put("ltekeycount", cnt);

            this.matchList.add(map);
        }

        // 대표적인 것은 가장 처음에 있는 값이 됨
        keyList = list.get(0).getString(telecomName);
        keyList = UtilString.removeNullCsv(keyList, ",");
        pciList = list.get(0).getString("pci");
        channelList = list.get(0).getString("channel");
        celltypeList = list.get(0).getString("celltype");
        count = list.get(0).getInt("ltekeycount");

        if(list.get(0).containsKey("wifimatchKey")){
            wifiMatchKey = list.get(0).getString("wifimatchKey");
        }else{
            wifiMatchKey = list.get(0).getString("maclist");
        }
        lteMatchKey = list.get(0).getString("ltematchKey");

    }
    
    public void resetData(String keyStr){
        resetData(keyStr, false);
    }
    
    public void resetData(String keyStr, boolean distictData){
        
        if(distictData){
            keyStr = LTEKeyParser.makeDistinctKey(keyStr);
        }
        
        setData(this.telecomName, keyStr);
        
    }
    
//    @Override
//    public String toString() {
//        
//        return "[" + controllerCount + "] id:" + id + ", version:" + version +", template:" + template + ", controllerPage:" + controllerPage  + ", commandClass:" + commandClass  + ", commandName:" + commandName;
//    }
    
    
}
