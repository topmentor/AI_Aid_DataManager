/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.AppConfig;


/**
 *
 * @author dreamct
 */
public class OneTimePassword {

    private int passwordConfirmCount = 0;
    private int passwordSendCount = 0;
    private long createdTime = 0;
    private String password = "";

    public OneTimePassword() {
        this.password = UtilSystem.createRandom4Number() + "";
        this.createdTime = System.currentTimeMillis();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public String getPassword() {
        return password;
    }

    public boolean isValidPassword(String userPswd) {
        this.passwordConfirmCount++;
        if (userPswd == null) {
            return false;
        }
        if (this.password.equals(userPswd)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isValidTimeLimit() {
        if (this.passwordConfirmCount > 3) {
            return false;
        }
        long use = System.currentTimeMillis() - this.createdTime;
        if (use > 180 * 1000) { //3분
            return false;
        } else {
            return true;
        }
    }

    public int sendPassword(String userCellPhone) {
        this.passwordSendCount++;
        if (this.passwordSendCount <= 3) {
            this.passwordConfirmCount = 0;
            this.createdTime = System.currentTimeMillis();//시간을 다시 설정한다.
            String msg = AppConfig.getConf("site_domain") + " 회원 인증을 위한 인증번호 : " + this.password;
//            SmsThread.getDefaultMessageSender().postMessage(new Sms(AppConfig.getConf("sms_sender_number"), userCellPhone, msg));
            // @@ to-do 이메일 전송으로 대체 필요
        }
        return this.passwordSendCount;
    }
    
    public int getPasswordConfirmCount(){
        return this.passwordConfirmCount;
    }
}