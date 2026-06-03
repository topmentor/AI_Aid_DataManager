/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.base;

import com.ithows.BaseDebug;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Form 방식의 컨트롤러에 사용되며 successView 와 onSubmit함수를 제공하는 abstract 클래스
 * @author dreamct2
 */
public class CommandManager {

    protected String commandClass = null;
    protected String commandName = null;

    /**
     * commandClass 값을 설정한다.
     * @param commandClass xml에서 설정한 commandClass
     */
    public void setCommandClass(String commandClass) {
        this.commandClass = commandClass;
    }

    /**
     *commandName 값을 설정한다.
     * @param commandName xml에서 설정한 commandName
     */
    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    /**
     * commandName 값을 얻는다.
     * @return commandName을 반환한다.
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * commandClass 와 commandName을 이용하여 객체 생성후 세션에 넣어준다.
     * @param request 현재 request 서블릿
     * @param response 현재 request 서블릿
     */
    public void setRequestResponse(HttpServletRequest request, HttpServletResponse response) {
        if (this.commandClass == null || this.commandName == null) {
            return;
        }
        try {
            Class c = Class.forName(this.commandClass);
            Object obj = c.newInstance();
            //힘든작업
            //ParameterName과 함수명이 일치할 때 request의 Parameter를 obj에 Reflection 방식으로 set한다. //문자와 숫자 정도만 구분
            Enumeration en = request.getParameterNames();


            while (en.hasMoreElements()) {
                String sname = (String) en.nextElement();
                String methodName = makeMethodName(sname);
                Method m = this.getMethod(c, methodName);
                String value = (String) request.getParameter(sname);
                try{
                    if (m != null) {
                        Class[] parameterTypes = m.getParameterTypes();
                        Object obj2 = null;
                        if (parameterTypes[0].equals(String.class)) {
                            obj2 = value;
                        } else if (parameterTypes[0].equals(int.class)) {
                            obj2 = Integer.parseInt(value.replace(",",""));
                        } else if (parameterTypes[0].equals(float.class)) {
                            obj2 = Float.parseFloat(value);
                        } else if (parameterTypes[0].equals(long.class)) {
                            obj2 = Long.parseLong(value);
                        } else if (parameterTypes[0].equals(double.class)) {
                            obj2 = Double.parseDouble(value);
                        } else if (parameterTypes[0].equals(char.class)) {
                            obj2 = value.charAt(0);
                        }
                        Object[] paramValues = new Object[]{obj2};
                        m.invoke(obj, paramValues);
                    }
                }catch(Exception e){
                     BaseDebug.log(e,  "위험: " + sname + "의 값이 잘못되었습니다. 값:[" + value +"]");
                }
            }
            request.setAttribute(this.commandName, obj);
        } catch (Exception ex) {
             BaseDebug.log(ex,  "Command 객체를 생성하던 중 에러가 발생하였다. " + this.commandClass + " 클래스가 없거나 함수 매칭이 잘못되었다.");
        }
    }

    /**
     * 동적으로 생성한 클래스 정보와 함수명을 받아 매개변수의 타입과 형태를 판단하여 적당한 함수를 반환한다.
     * @param c 동적으로 생성한 클래스
     * @param methodName 얻을 함수명
     * @return 동적으로 생성한 클래스에 존재하는 함수를 반환한다.
     */
    //오버로딩은 없다.
    private Method getMethod(Class c, String methodName) {
        Method m = null;
        Method[] mar = c.getMethods();
        for (int i = 0; i < mar.length; i++) {
            Class[] params = mar[i].getParameterTypes();
            if (mar[i].getName().equals(methodName) && params.length == 1) {//파라미터가 하나이면서 함수명이 같은 함수를 리턴한다.
                m = mar[i];
                break;
            }
        }
        return m;
    }

    /**
     * property명을 받아 완성된 함수명으로 반환해준다.
     * @param method XML에 설정한 property name
     * @return 변환된 메서드명를 반환한다.
     */
    private String makeMethodName(String method) {
        StringTokenizer st = new StringTokenizer(method);
        String str = "";
        while (st.hasMoreElements()) {
            str = st.nextToken();
            str = str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
        }
        str = "set" + str;
        return str;
    }
}
