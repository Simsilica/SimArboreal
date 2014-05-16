/*
 * ${Id}
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

package com.simsilica.arboreal.mesh;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.simsilica.arboreal.Segment;
import com.simsilica.arboreal.Tree;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 *  Simple tree mesh generator that simply generates lines for
 *  the branches.
 *
 *  @author    Paul Speed
 */
public class LineMeshGenerator {

    public Mesh generateMesh( Tree tree ) {
    
        List<Vector3f> points = new ArrayList<Vector3f>();
 
        Vector3f base = new Vector3f();
        for( Segment seg : tree ) {
            if( seg == null ) {
                continue;
            }       
            addBranches(base, seg, points);
        }
        
        Mesh mesh = new Mesh();           
        mesh.setMode(Mesh.Mode.Lines);
        FloatBuffer fb = BufferUtils.createFloatBuffer(points.toArray(new Vector3f[0]));
        mesh.setBuffer(VertexBuffer.Type.Position, 3, fb);
        mesh.updateBound();

        return mesh;                
    }
    
    protected void addBranches( Vector3f start, Segment seg, List<Vector3f> points ) {
        
        points.add(start);
        Vector3f end = start.add(seg.dir.mult(seg.length));
        points.add(end);
        
        for( Segment child : seg ) {
            addBranches(end, child, points);
        }
    }
}
