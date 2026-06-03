/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ithows.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author dreamct
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ControllerClassInfo {
        public String controllerPage();
}
