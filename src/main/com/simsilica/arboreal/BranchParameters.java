/*
 * Copyright (c) 2014, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.arboreal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 *  @author    Paul Speed
 */
public class BranchParameters {
    private static final String VERSION_KEY = "formatVersion";
    private static final int VERSION = 1;

    public boolean enabled;
    public boolean inherit;
 
    public float radiusScale;
    public float lengthScale;
    public int radialSegments;
    public int lengthSegments;
    public float taper;
    public float inclination;
    public float twist;
    public float tipRotation;
 
    public float segmentVariation;   
    public float gravity;
    
    public boolean hasEndJoint;
    public int sideJointCount;
    public float sideJointStartAngle; 
    
    public BranchParameters() {
        this.enabled = true;
        this.inherit = true;
        this.radiusScale = 1f;
        this.lengthScale = 0.6f;
        this.radialSegments = 6;
        this.lengthSegments = 4;
        this.taper = 0.7f;
        this.segmentVariation = 0.4f;
        this.gravity = 0.1f;
        
        this.sideJointCount = 4;
        this.inclination = 0.872f;
    }

    public void setEnabled( boolean enabled ) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
 
    public static void main( String... args ) {
        
        BranchParameters test = new BranchParameters();
        
        Map<String, Object> map = test.toMap();
        System.out.println( "Map:" + map );
 
        test.enabled = false;
        test.inherit = false; 
        test.radiusScale = 0;
        test.lengthScale = 0;
        test.radialSegments = 0;
        test.lengthSegments = 0;
        test.taper = 0;
        test.inclination = 0;
        test.twist = 0;
        test.tipRotation = 0;
 
        test.segmentVariation = 0;   
        test.gravity = 0;
    
        test.hasEndJoint = false;
        test.sideJointCount = 0;
        test.sideJointStartAngle = 0;

        Map<String, Object> map2 = test.toMap();
        System.out.println( "Map2:" + map2 );

        test.fromMap(map);
        
        Map<String, Object> map3 = test.toMap();
        System.out.println( "Map3:" + map3 );
        
    }
 
    public void fromMap( Map<String, Object> map ) {
        Number version = (Number)map.get(VERSION_KEY);
 
        Class type = getClass();       
        for( Map.Entry<String, Object> e : map.entrySet() ) {
            if( VERSION_KEY.equals(e.getKey()) ) {
                continue;
            }
            try {
                Field f = type.getField(e.getKey());
                if( f.getType() == Boolean.TYPE ) {
                    f.set(this, e.getValue());
                } else if( f.getType() == Integer.TYPE ) {
                    Number val = (Number)e.getValue();
                    f.set(this, val.intValue());
                } else if( f.getType() == Float.TYPE ) {
                    Number val = (Number)e.getValue();
                    f.set(this, val.floatValue());
                } else {
                    throw new RuntimeException("Unhandled type:" + f.getType());
                }
            } catch( Exception ex ) {
                throw new RuntimeException("Error processing:" + e, ex); 
            }
        }               
    }
    
    public Map<String, Object> toMap() {
 
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put(VERSION_KEY, VERSION);       
        // Easy for this one
        for( Field f : getClass().getFields() ) {
            try {
                result.put(f.getName(), f.get(this));
            } catch( Exception e ) {
                throw new RuntimeException("Error getting field:" + f, e);
            }
        } 
        return result;
    }    
}


