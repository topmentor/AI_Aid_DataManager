/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ithows.base;

import com.ithows.PageBean;
import java.util.HashMap;

/**
 *
 * @author dreamct2
 */
public class PageBeanContainer extends HashMap{

    public PageBean get(String id) {
        return (PageBean)super.get(id);
    }

    public void add(String id, PageBean p) {
        super.put(id, p);
    }
}
