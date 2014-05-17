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

import com.jme3.math.Vector3f;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *  Represent a section of a trunk, branch, or root.
 *
 *  @author    Paul Speed
 */
public class Segment implements Iterable<Segment> {
    
    public enum ConnectionType { Curve, Extrude, Abut };

    public float startRadius;
    public float endRadius;
    public float length;
    public float uScale;
    public float vStart;
    public float vEnd;
    public float twist;
    public int radials = 3;
    public Vector3f dir = new Vector3f(0, 1, 0);
    public ConnectionType parentConnection = ConnectionType.Extrude;
    public Segment[] children;
    
    public Segment() {
    }
 
    public Segment( int childCount ) {
        this.children = new Segment[childCount];
    }
 
    public boolean isInverted() {
        return vStart > vEnd;
    }
 
    public Segment extend( ConnectionType connectionType, boolean asOnlyChild ) {
        Segment result = new Segment();
        result.parentConnection = connectionType;
        result.startRadius = this.endRadius;
        result.uScale = this.uScale;
        result.vStart = this.vEnd;
        
        this.children = new Segment[] { result };
        return result;
    } 
    
    @Override
    public Iterator<Segment> iterator() {
        if( children == null ) {
            List<Segment> empty = Collections.emptyList();
            return empty.iterator();
        }
        return Arrays.asList(children).iterator();
    }
}


