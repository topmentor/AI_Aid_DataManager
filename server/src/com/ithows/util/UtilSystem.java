/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.BaseDebug;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 *
 * @author dreamct
 */
public class UtilSystem {

    public static int createRandom4Number() {
        Random rand = new Random();
        int authNum = rand.nextInt(8999);
        authNum += 1000; // 1000~ 9999 사이의 숫자 생성
        return authNum;
    }

    public static int parseInt(String _authNum, int defaultVal) {
        int authNum = defaultVal;
        try {
            authNum = Integer.parseInt(_authNum);
            if (_authNum.trim().length() != 4) { //4자리 숫자인지 확인
                authNum = -1;
            }
        } catch (Exception e) {
            BaseDebug.log(e);
        }
        return authNum;
    }

    public static void screenCapture(String imgPath, String imgName) {

        try {

            Robot robot = new Robot();

            //모니터 화면 크기 가지고 오는 객체
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

            //모니터의 화면을 selectRect에 가로,세로 표현
            Rectangle selectRect = new Rectangle((int) screen.getWidth(), (int) screen.getHeight());

            BufferedImage buffimg = robot.createScreenCapture(selectRect);

            File screenfile = new File(imgPath + "/" + imgName + ".jpg");

            ImageIO.write(buffimg, "jpg", screenfile);

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}
