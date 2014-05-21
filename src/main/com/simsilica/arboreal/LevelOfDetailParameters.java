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


/**
 *  Defines the level of detail settings for a particular
 *  tree model and a particular level of detail.
 *
 *  @author    Paul Speed
 */
public class LevelOfDetailParameters {
 
    public enum ReductionType { 
    
        None("None"), FlatPoly("Flat-poly"), Impostor("Impostor"); 
        
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
        this(0, ReductionType.None, Integer.MAX_VALUE, Integer.MAX_VALUE, 6);        
    }
    
    public LevelOfDetailParameters( float distance, ReductionType reduction, int branchDepth,
                                    int rootDepth, int maxRadialSegments ) {
        this.distance = distance;
        this.reduction = reduction;
        this.branchDepth = branchDepth;
        this.rootDepth = rootDepth;
        this.maxRadialSegments = maxRadialSegments;
    }
 
    @Override   
    public String toString() {
        return "LOD[distance=" + distance + ", reduction=" + reduction 
                              + ", branchDepth=" + branchDepth
                              + ", rootDepth=" + rootDepth 
                              + "]";
    }   
}
