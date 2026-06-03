/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ithows;

import com.ithows.base.TemplateBean;

/**
 *
 * @author dreamct2
 */
public class PageBean {
    private String id = "";
    private TemplateBean template= null;
    private String controllerPage = "";
    private Object controller = null;
    private String commandClass = "";
    private String commandName = "";
    private String methodName = "";
    private static int controllerCount = 0;
    private int version = 0;
    private boolean loginRequired = false;
    private int requiredSecurityLevel = 0;
    private boolean apiKeyRequired = false;

    public static int getControllerCount() {
        return controllerCount;
    }

    public static void setControllerCount(int controllerCount) {
        PageBean.controllerCount = controllerCount;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isLoginRequired() {
        return loginRequired;
    }

    public void setLoginRequired(boolean loginRequired) {
        this.loginRequired = loginRequired;
    }

    public int getRequiredSecurityLevel() {
        return requiredSecurityLevel;
    }

    public void setRequiredSecurityLevel(int requiredSecurityLevel) {
        this.requiredSecurityLevel = requiredSecurityLevel;
    }

    public boolean isApiKeyRequired() {
        return apiKeyRequired;
    }

    public void setApiKeyRequired(boolean apiKeyRequired) {
        this.apiKeyRequired = apiKeyRequired;
    }


    @Override
    public String toString() {
        controllerCount++;
        return "[" + controllerCount + "] id:" + id + ", version:" + version +", template:" + template + ", controllerPage:" + controllerPage  + ", commandClass:" + commandClass  + ", commandName:" + commandName;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getId(){
        return this.id;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public String getControllerPage() {
        return controllerPage;
    }

    public void setControllerPage(String controller) {
        this.controllerPage = controller;
    }

    public String getCommandClass() {
        return commandClass;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandClass(String commandClass) {
        this.commandClass = commandClass;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public TemplateBean getTemplate() {
        return template;
    }

    public void setTemplate(TemplateBean template) {
        this.template = template;
    }
}
