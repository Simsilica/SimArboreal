/*
 * $Id$
 *
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
 *  Defines the level of detail settings for a particular
 *  tree model and a particular level of detail.
 *
 *  @author    Paul Speed
 */
public class LevelOfDetailParameters {
    private static final String VERSION_KEY = "formatVersion";
    private static final int VERSION = 1;

    public enum ReductionType { 
    
        Normal("Normal"), FlatPoly("Flat-poly"), Impostor("Impostor"); 
        
        private String name;
        
        private ReductionType( String name ) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    };
    
    /**
     *  The distance from the tree in which this
     *  level of detail takes affect. 
     */
    public float distance;
 
    /**
     *  The type of mesh reduction that will be used at this level of
     *  detail.
     */
    public ReductionType reduction;
        
    /**
     *  The number of branch levels to render at this level
     *  of detail.
     */
    public int branchDepth;
    
    /**
     *  The number of root levels to render at this level
     *  of detail.
     */
    public int rootDepth;
 
    /**
     *  The maximum number of radials allowed at this level of detail.
     */
    public int maxRadialSegments;
 
    
    public LevelOfDetailParameters() {
        this(0, ReductionType.Normal, Integer.MAX_VALUE, Integer.MAX_VALUE, 6);        
    }
    
    public LevelOfDetailParameters( float distance, ReductionType reduction, int branchDepth,
                                    int rootDepth, int maxRadialSegments ) {
        this.distance = distance;
        this.reduction = reduction;
        this.branchDepth = branchDepth;
        this.rootDepth = rootDepth;
        this.maxRadialSegments = maxRadialSegments;
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
                } else if( f.getType() == ReductionType.class ) {
                    f.set(this, Enum.valueOf(ReductionType.class, (String)e.getValue()));
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
                if( f.getType() == ReductionType.class ) {
                    result.put(f.getName(), ((ReductionType)f.get(this)).name());
                } else {
                    result.put(f.getName(), f.get(this));
                }
            } catch( Exception e ) {
                throw new RuntimeException("Error getting field:" + f, e);
            }
        } 
        return result;
    }    
 
    @Override   
    public String toString() {
        return "LOD[distance=" + distance + ", reduction=" + reduction 
                              + ", branchDepth=" + branchDepth
                              + ", rootDepth=" + rootDepth 
                              + "]";
    }   
}
