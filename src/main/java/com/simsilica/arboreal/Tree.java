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

import java.util.Arrays;
import java.util.Iterator;


/**
 *
 *  @author    Paul Speed
 */
public class Tree implements Iterable<Segment> {
 
    private static final int TRUNK_INDEX = 0;   
    private static final int ROOTS_INDEX = 1;   
    private Segment[] children;
    
    public Tree() {
        this.children = new Segment[2];
    }
 
    @Override
    public Iterator<Segment> iterator() {
        return Arrays.asList(children).iterator();
    }
 
    public Segment setTrunk( Segment segment ) {
        return setSegment(TRUNK_INDEX, segment);
    }
    
    public Segment getTrunk() {
        return children[TRUNK_INDEX];
    }
    
    public Segment setRoots( Segment segment ) {
        return setSegment(ROOTS_INDEX, segment);
    }
    
    public Segment getRoots() {
        return children[ROOTS_INDEX];
    }
    
    public Segment setSegment( int index, Segment segment ) {
        this.children[index] = segment;
        return segment;        
    }
    
    public Segment getSegment( int index ) {
        return children[index];
    }
    
    public int getSegmentCount() {
        return children.length;
    }
    
    public Segment[] getSegments() {
        return children;
    }
    
    @Override
    public String toString() {
        return "Tree[" + Arrays.asList(children) + "]";
    }
}


