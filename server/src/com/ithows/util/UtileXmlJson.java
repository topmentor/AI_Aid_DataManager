/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.ithows.util;

/**
 * Class UtileXmlJson
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

public class UtileXmlJson {


    public static void saveXml(String xmlFName, JSONObject jsondata) throws JSONException,IOException {
        String xmlStr = jsonToXml(jsondata);
        String saveName = xmlFName;
        File file1 = new File(xmlFName);

        if (file1.isFile()) {
            // System.out.println("OK 파일 있습니다.");
            saveName = (xmlFName.substring(0, xmlFName.length() - 4) + "_" + new SimpleDateFormat("YYYYMMdd_HHmmss").format(Calendar.getInstance().getTime()) + ".xml");;
            File file2 = new File(saveName);
            if (!file1.renameTo(file2)) {
                System.err.println("Cann't rename file : " + file1);
            }

        }

        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(xmlFName), "UTF-8"));
        try {
            out.write(xmlStr);
        } finally {
            out.close();
        }
    }


    public static JSONObject xmlFileToJson(String xmlFName) throws JSONException, IOException {
        String line = "", xmlStr = "";
        JSONObject jsondata = null;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(xmlFName));
            while ((line = br.readLine()) != null) {
                xmlStr += line;
            }
            br.close();
            jsondata = XML.toJSONObject(xmlStr);

        } catch (Exception e) {

        } finally {
            if (br != null) {
                br.close();
            }
        }
        return jsondata;
    }


    public static JSONObject xmlStringToJson(String xmlStr) throws JSONException, IOException {
        JSONObject jsondata = null;

        try {
            jsondata = XML.toJSONObject(xmlStr);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsondata;
    }

    
    public static boolean jsonToFile(String jsonStr, String jsonFilName) {
        try (FileWriter writer = new FileWriter(jsonFilName)) {
            writer.append(jsonStr);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
    
    public static String jsonToXml(JSONObject jsondata) throws JSONException {

        String xmlStr = XML.toString(jsondata);
        int indent = 4;

        try {
            Source xmlInput = new StreamSource(new StringReader(xmlStr));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            // This statement works with JDK 6
            transformerFactory.setAttribute("indent-number", indent);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Throwable e) {
            // You'll come here if you are using JDK 1.5
            // you are getting an the following exeption
            // java.lang.IllegalArgumentException: Not supported: indent-number
            // Use this code (Set the output property in transformer.
            try {
                Source xmlInput = new StreamSource(new StringReader(xmlStr));
                StringWriter stringWriter = new StringWriter();
                StreamResult xmlOutput = new StreamResult(stringWriter);
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
                transformer.transform(xmlInput, xmlOutput);
                return xmlOutput.getWriter().toString();
            } catch (Throwable t) {
                return xmlStr;
            }
        }
    }

    public Properties parseProperties(String s) {
        // grr at load() returning void rather than the Properties object
        // so this takes 3 lines instead of "return new Properties().load(...);"
        final Properties p = new Properties();
        try {
            p.load(new StringReader(s));
        } catch (IOException ex) {
            Logger.getLogger(UtileXmlJson.class.getName()).log(Level.SEVERE, null, ex);
        }
        return p;
    }
}
