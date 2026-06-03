/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.util;

import com.ithows.FileInfo;
import com.ithows.BaseDebug;
import com.ithows.JakartaUpload;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author JJanga
 */
public class ImageUtil {

    private static final int UPLOAD_STD_SIZE = 1440;

    public static BufferedImage rotateBufferedImage(BufferedImage bi) {
        BufferedImage bi2 = new BufferedImage(bi.getHeight(), bi.getWidth(), bi.getType());
        AffineTransform tx = new AffineTransform();
        tx.translate(bi.getHeight() / 2, bi.getWidth() / 2);
        tx.rotate(Math.toRadians(90.0));//(radian,arbit_X,arbit_Y)
        tx.translate(-bi.getWidth() / 2, -bi.getHeight() / 2);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        op.filter(bi, bi2);//(sourse,destination)
        return bi2;
    }

    public static String autoSaveFormFile(HttpServletRequest request, JakartaUpload mRequest, String formFileinputName, String uploadDir, int manNo, int corpNo, String option) {

        String fileName = null;
        String contentType = request.getContentType();
        if ((contentType == null) || (contentType.indexOf("multipart/form-data") == -1)) {
            BaseDebug.info("Invalid Content Type!!!!");
            return null;
        }
        FileInfo fileInfo = mRequest.getFileInfo(formFileinputName);
        if (fileInfo != null) {
            fileName = fileInfo.getFileName();
            fileName = reSaveAsImage(uploadDir, fileName, manNo, corpNo, option);//이미지가 너무 크면 줄여서 저장한다.
        }
        return fileName;
    }

    private static String reSaveAsImage(String dir, String fileName, int no, int corpNo, String option) {
        /*
         * option = nomu, corp, ownerCorp, ownerStamp
         */
        String targetName = null;
        try {
            File file = new File(dir + fileName);
            BufferedImage biSrc = ImageIO.read(file);

            if (option == null) {
                option = "scan";
            }
            targetName = getNewTargetFileName(corpNo, no, option);
            int width = biSrc.getWidth();
            int height = biSrc.getHeight();
            /*
             * 어떤 경로로 업로드 되는 이미지던지 큰 쪽이 1024 이하는 업로드 가능하다.(주민증도 가능)
             */
            if (width > height && width > UPLOAD_STD_SIZE) { //어느 한쪽이 클 경우 1024보다 크다면 큰 쪽은 1024로, 나머지는 1024를 기준으로 줄여준다.                                  
                height = getStandardSize(width, height);
                width = UPLOAD_STD_SIZE;
            } else if (height > width && height > UPLOAD_STD_SIZE) {
                width = getStandardSize(height, width);
                height = UPLOAD_STD_SIZE;
            }
            BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(biSrc, 0, 0, width, height, null);
            g.dispose();
            ImageIO.write(resizedImage, "JPG", new File(dir + targetName));
            if (file.exists()) {
                file.delete();
            }
        } catch (IOException e) {
            BaseDebug.log(e);
        }
        return targetName;
    }
    //이미지 사이즈 줄이기.(먼저 들어온 것_더 큰쪽_ 기준으로 줄이기)

    private static int getStandardSize(int n1, int n2) {
        float m1 = (float) n1;
        float m2 = (float) n2;
        float ratio = ((float) UPLOAD_STD_SIZE / m1) * m2;
        return (int) ratio;
    }
    //어느쪽에서 업로드 되는 이미지 인지 구분하여 파일명으로 만들어준다. 

    private static String getNewTargetFileName(int root, int id, String option) {
        String targetName = root + "_" + option + "_" + id + "_" + System.currentTimeMillis();
        targetName += ".jpg";
        return targetName;
    }
}
