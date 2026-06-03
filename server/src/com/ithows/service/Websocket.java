/*
 *  Copyright 2020. S.O.X. All rights reserved
 */

package com.ithows.service;

/**
 * Class Websocket
 * 
 * @author Roi Kim <S.O.X Co. Ltd.>
 */


import java.io.IOException;
import java.util.Collections;	
import java.util.HashMap;	
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;	
import javax.websocket.OnClose;	
import javax.websocket.OnError;	
import javax.websocket.OnMessage;	
import javax.websocket.OnOpen;	
import javax.websocket.Session;	
import javax.websocket.server.ServerEndpoint;	
import org.json.JSONObject;
 	
// handshake 설정하기 위한 클래스를 지정한다.	
@ServerEndpoint(value = "/websocket", configurator = HttpSessionConfigurator.class)	
public class Websocket {	
    private Map<Session, EndpointConfig> configs = Collections.synchronizedMap(new HashMap<>());	
    // handshake가 끝나면 handleOpen이 호출된다.	

    private static Session admin = null;

    @OnOpen	
    public void handleOpen(Session userSession, EndpointConfig config) {	
      System.out.println("client is now connected..." + userSession.getId());	

      if(admin!=null){
          try {
              admin.close();
          } catch (IOException ex) {
              System.out.println(ex.getLocalizedMessage());
          }
      }

      // tomcat은 timeout이 20초(20000)으로 설정 되어 있음
      // The write timeout used when sending WebSocket messages in blocking mode defaults to 20000 milliseconds (20 seconds)
      userSession.setMaxIdleTimeout(0);    
      
      // binary의 buffer size를 설정한다. 혹시라도 파일 업로드 서버를 만들고 싶다고 한다면 이 설정을 확인해야 한다.    
      userSession.setMaxBinaryMessageBufferSize(1024 * 1024 * 10);

      admin = userSession;

      // EndpointConfig의 클래스를 위 맵에 넣는다.	
      if (!configs.containsKey(userSession)) {	
        // userSession 클래스는 connection이 될 때마다 인스턴스 생성되는 값이기 때문에 키로서 사용할 수 있다.	
        configs.put(userSession, config);	
      }	


    }	

    // 클라이언트로 부터 메시지가 오면 handleMessage가 호출 된다.	
    @OnMessage	
    public String handleMessage(String message, Session userSession) {	
      // 위 맵으로 부터 userSession을 키로 EndpointConfig값을 가져온다.	
      if(!message.equals("Get")){
          return "echo " + message;
      }else if (configs.containsKey(userSession)) {	
        EndpointConfig config = configs.get(userSession);	
        // HttpSessionConfigurator에서 설정한 session값을 가져온다.	
        HttpSession session = (HttpSession) config.getUserProperties().get(HttpSessionConfigurator.Session);	
        // Session의 TestSession키로 데이터를 가져온다. (테스트용)	
        return "Session - " + (String) session.getAttribute("TestSession");	
      }	
      return "error " ;	
    }	



    // 바이너리 통신의 경우 
    @OnMessage
    public byte[] handleMessage(byte[] message, Session session) {

      if(message.length <= 125) {
        String msg = new String(message);
        System.out.println(msg);
        msg = "echo - " + msg;
        return msg.getBytes();
      }else {
        String msg = new String(message);
        System.out.println(message.length);
        System.out.println(msg.substring(msg.length() - 10));
        return message;
      }
    }
    

    @OnClose	
    public void handleClose(Session userSession) {	
      System.out.println("client is now disconnected...");	
      // 접속이 종료되면 map에서 EndpointConfig를 제거한다.	
      if (configs.containsKey(userSession)) {	
        configs.remove(userSession);	
      }
      
      if(admin!=null){
          try {
              admin.close();
              admin = null;
          } catch (IOException ex) {
              System.out.println(ex.getLocalizedMessage());
          }
      }
      
    }	
    
    @OnError	
    public void handleError(Throwable e, Session userSession) {	
      e.printStackTrace();	
    }	
  
    // 운영자 유저로 메시지를 보내는 함수
    public static void send(String message) {
        if (admin != null) {
            try {
                admin.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void send(JSONObject message) {
        send(message.toString());
    }
    
    // 세션을 닫음 (서버이므로 소켓은 열어둠)
    public static void sessionClose() {
        if (admin != null) {
            try {
                admin.close();
                admin = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

 
}