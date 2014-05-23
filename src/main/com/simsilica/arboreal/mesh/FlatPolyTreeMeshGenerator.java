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

package com.simsilica.arboreal.mesh;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.simsilica.arboreal.LevelOfDetailParameters;
import com.simsilica.arboreal.Segment;
import com.simsilica.arboreal.Segment.ConnectionType;
import com.simsilica.arboreal.Tree;
import java.nio.FloatBuffer;
import java.util.List;


/**
 *
 *  @author    Paul Speed
 */
public class FlatPolyTreeMeshGenerator {

    private CurveGenerator curveGen = CurveGenerator.DEFAULT;
    
    public Mesh generateMesh( Tree tree, LevelOfDetailParameters lod, 
                              float yOffset, int uRepeat, float vScale, 
                              List<Vertex> tips ) {
 
        MeshBuilder mb = new MeshBuilder();
        
        Segment trunk = tree.getTrunk();        
        Vector3f center = new Vector3f(0, yOffset, 0);
 

        // The flat poly axis-oriented billboards are structured as
        // quads where each position pair rotates about a local axis
        // specified in the normal.  Size defines both radius and offset
        // while the texture coordinate is used normally. 
        // The position pairs themselves share the same location... the
        // center of the branch.
        //
        // For our purposes, we will continue the branch for extrusion
        // but will start a new chain for curves and abutments.  It's not
        // possible to properly "direction" the join for multiple branch
        // points.
 
        // parms: float x, float y, float z, float u, float v, int group, float epsilon         
        Vertex base1 = mb.createVertex(-trunk.startRadius, yOffset, 0, 0, 0, 0, -1);
        base1.normal = new Vector3f(0, 1, 0); 
        base1.weight = trunk.startRadius;
        Vertex base2 = mb.createVertex(+trunk.startRadius, yOffset, 0, uRepeat * 0.5f, 0, 0, -1);
        base2.normal = base1.normal;
        base2.weight = trunk.startRadius;
 
                
        // Note: we coopt weight for 'size' because MeshBuilder doesn't support
        //       size directly and it's a little too specific to add in my opinion.
        //       Since we won't be smoothing we can use weight for ourselve and
        //       fix the sizes after mesh creation. 
 
        for( Segment seg : tree ) {
            if( seg == null ) {
                continue;
            }
            
            if( seg.isInverted() ) {
                renderSegment(center, base1, base2, seg, 0, uRepeat, -vScale, lod, 0, mb, null);
            } else {
                renderSegment(center, base1, base2, seg, 0, uRepeat, vScale, lod, 0, mb, tips);
            }
        }
 
        Mesh result = mb.build();
        
        // Now build the size buffer from the vertexes
        List<Vertex> verts = mb.getVertexes();
        FloatBuffer sb = BufferUtils.createFloatBuffer(verts.size());
        for( Vertex v : verts ) {
System.out.println( "v weight:" + v.weight );           
            sb.put(v.weight);
        }
        result.setBuffer(Type.Size, 1, sb);
 
        return result;
    }

    protected boolean renderDepth( int depth, boolean inverted, LevelOfDetailParameters lod ) {    
        if( inverted && depth < lod.rootDepth ) {
            return true;
        } else if( !inverted && depth < lod.branchDepth ) {
            return true;
        } else {
            return false;
        }
    } 
    
    protected void renderSegment( Vector3f center, Vertex base1, Vertex base2, Segment seg, 
                                  float vBase, int uRepeat, float vScale,
                                  LevelOfDetailParameters lod, int depth,  
                                  MeshBuilder mb, List<Vertex> tips ) {
                                  
        // Calculate the next center location
        // If we don't render we'll still need to pass it along for the
        // tip and we don't want to create mesh builder vertexes and then
        // not use them.

        Vector3f next = center.add(seg.dir.mult(seg.length));
        Vertex tip1 = null;
        Vertex tip2 = null;
        
        float vScaleLocal = vScale * (1 / seg.endRadius);
        vBase += seg.length * vScaleLocal;
        
        boolean renderDepth = renderDepth(depth, seg.isInverted(), lod);
        
        if( renderDepth ) {
            // So... how we render this level is actually dependent on whether we're
            // extending to the next level or not (extrusion) since we'll need to "average"
            // the normals.
        
            // See if there is an extusion that can define the shared dir
            Vector3f tipDir = seg.dir;
            for( Segment child : seg ) {
                if( child.parentConnection == ConnectionType.Extrude ) {
                    tipDir = seg.dir.add(child.dir).mult(0.5f).normalizeLocal();
                    break;
                }
            }
            
            // Now we can properly define the new tips and add this segment's quad
            tip1 = mb.createVertex(next.x - seg.endRadius, next.y, next.z, 0, vBase, 0, -1);
            tip1.weight = seg.endRadius;
            tip2 = mb.createVertex(next.x + seg.endRadius, next.y, next.z, uRepeat * 0.5f, vBase, 0, -1);
            tip2.weight = seg.endRadius;
 
            if( vScale > 0 ) {
                tip1.normal = tipDir; 
                tip2.normal = tipDir; 
                mb.addTriangle(base1, base2, tip2);
                mb.addTriangle(base1, tip2, tip1);
            } else {
                // We're extending down 
                mb.addTriangle(tip1, tip2, base2);
                mb.addTriangle(tip1, base2, base1);
                tip1.normal = tipDir.negate(); 
                tip2.normal = tip1.normal;
            } 
        }            
 
        if( !seg.hasChildren() ) {
        
            if( tips != null ) {            
                // Add the tip and be done
                Vertex branchTip = new Vertex(next);
                branchTip.normal = seg.dir;
                tips.add(branchTip);
            }
            return;
        }
 
        boolean renderNextDepth = renderDepth;
        if( !renderDepth(depth + 1, seg.isInverted(), lod) ) {
            renderNextDepth = false;             
        }                    
                
        for( Segment child : seg ) {
            switch( child.parentConnection ) {
                case Extrude:
                    // We can just continue directly
                    renderSegment(next, tip1, tip2, child, vBase, uRepeat, vScale, lod, depth, mb, tips);
                    break;
                case Abut:
                    throw new UnsupportedOperationException("Abutment not yet supported.");
                case Curve:
                    List<CurveStep> steps = curveGen.generateCurve(seg.dir, seg.endRadius,
                                                                   child.dir, child.startRadius,
                                                                   vBase, vScale);
                    CurveStep last = steps.get(steps.size()-1);
                    Vector3f childCenter = next.add(last.center);
                    float v = vBase + last.v;
 
                    if( !renderNextDepth ) {
                        // Then just push through
                        renderSegment(childCenter, null, null, child, v, uRepeat, vScale, lod, depth + 1, mb, tips);
                    } else {                    
                        // Create some new bases for this child 
                        Vertex cBase1 = mb.createVertex(childCenter.x - child.startRadius, 
                                                        childCenter.y, 
                                                        childCenter.z,
                                                        0, v, 0, -1);
                        cBase1.weight = child.startRadius;
                        Vertex cBase2 = mb.createVertex(childCenter.x + child.startRadius, 
                                                        childCenter.y, 
                                                        childCenter.z,
                                                        uRepeat * 0.5f, v, 0, -1);
                        cBase2.weight = child.startRadius;
                        if( vScale > 0 ) {
                            cBase1.normal = child.dir;
                            cBase2.normal = child.dir;
                        } else {                                                        
                            cBase1.normal = child.dir.negate();
                            cBase2.normal = cBase1.normal;
                        }
                        renderSegment(childCenter, cBase1, cBase2, child, v, uRepeat, vScale, lod, depth + 1, mb, tips);
                    }                   
                    break;
            }
        }                                   
    }                                
    
}

/*

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, new float[] {
                        0, 0, 0,
                        0, 0, 0,
                        0, 10, 0,
                        0, 10, 0,
                        5, 15, 0,
                        5, 15, 0,
                        0, 10, 0,
                        0, 10, 0,
                        -5, 15, 0,
                        -5, 15, 0,
                        
                        0, 10, 0,
                        0, 10, 0,
                        0, 15, 5,
                        0, 15, 5,
                        
                        0, 10, 0,
                        0, 10, 0,
                        0, 15, -5,
                        0, 15, -5
                    });       
        mesh.setBuffer(Type.Normal, 3, new float[] {
                        0, 1, 0,
                        0, 1, 0,
                        dir2.x, dir2.y, dir2.z,
                        dir2.x, dir2.y, dir2.z,
                        leg2.x, leg2.y, leg2.z,
                        leg2.x, leg2.y, leg2.z,
                        -dir2.x, dir2.y, dir2.z,
                        -dir2.x, dir2.y, dir2.z,
                        -leg2.x, leg2.y, leg2.z,
                        -leg2.x, leg2.y, leg2.z,
                        
                        dir2.z, dir2.y, dir2.x,
                        dir2.z, dir2.y, dir2.x,
                        leg2.z, leg2.y, leg2.x,
                        leg2.z, leg2.y, leg2.x,
                        dir2.z, dir2.y, -dir2.x,
                        dir2.z, dir2.y, -dir2.x,
                        leg2.z, leg2.y, -leg2.x,
                        leg2.z, leg2.y, -leg2.x
                    });
        mesh.setBuffer(Type.Size, 1, new float[] {
                        -1, 
                         1,
                        -0.7f,
                         0.7f,
                        -0.5f,
                         0.5f,
                        -0.7f,
                         0.7f,
                        -0.5f,
                         0.5f,
                         
                        -0.7f,
                         0.7f,
                        -0.5f,
                         0.5f,
                        -0.7f,
                         0.7f,
                        -0.5f,
                         0.5f
                    });
        mesh.setBuffer(Type.TexCoord, 2, new float[] {
                        0, 0,
                        1, 0,
                        0, 2.5f,
                        1, 2.5f,
                        0, 5,
                        1, 5,                                                
                        0, 2.5f,
                        1, 2.5f,
                        0, 5,
                        1, 5,
                                                
                        0, 2.5f,
                        1, 2.5f,
                        0, 5,
                        1, 5,                                                
                        0, 2.5f,
                        1, 2.5f,
                        0, 5,
                        1, 5                        
                    });
                    
                    
        //  4 5
        //  2 3
        //  0 1                    
        mesh.setBuffer(Type.Index, 3, new int[] {
                        0, 1, 3,
                        0, 3, 2,
                        2, 3, 5,
                        2, 5, 4,
                        6, 7, 9,
                        6, 9, 8,
                        
                        10, 11, 13,
                        10, 13, 12,
                        14, 15, 17,
                        14, 17, 16
                    });
*/
