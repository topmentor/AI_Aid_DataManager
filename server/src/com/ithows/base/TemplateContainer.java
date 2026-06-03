/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ithows.base;

import java.util.HashMap;

/**
 *
 * @author dreamct2
 */
public class TemplateContainer extends HashMap{
    public TemplateBean get(String id) {
        return (TemplateBean)super.get(id);
    }

    public void add(String id, TemplateBean t) {
        super.put(id, t);
    }
}
