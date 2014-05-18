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

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.simsilica.arboreal.Segment;
import com.simsilica.arboreal.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *  A tree mesh generator that skins the tree segments.
 *
 *  @author    Paul Speed
 */
public class SkinnedTreeMeshGenerator {

    public Mesh generateMesh( Tree tree, float yOffset, int uRepeat, float vScale, List<Vertex> tips ) {
 
        MeshBuilder mb = new MeshBuilder();
 
        // Create the base loop for the main trunk... all
        // other segments branch off of that one.        
        Segment trunk = tree.getTrunk();
        
        Vector3f center = new Vector3f(0, yOffset, 0);

        Quaternion up = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
        List<Vertex> baseLoop = mb.createLoop(center, up, trunk.startRadius, trunk.radials, 0, 0);
        List<Vertex> invertedLoop = null;

        mb.textureLoop(baseLoop, new Vector2f(0,0), new Vector2f(uRepeat, 0));        
        applyTangents(baseLoop, false);
        
           
        for( Segment seg : tree ) {
            if( seg == null ) {
                continue;
            }
            
            if( seg.isInverted() ) {
                if( invertedLoop == null ) {
                    invertedLoop = invertLoop(baseLoop);
                }
                addBranches(invertedLoop, seg, 0, -uRepeat, -vScale, mb, null);
            } else {
                addBranches(baseLoop, seg, 0, uRepeat, vScale, mb, tips);
            }
        }
 
        mb.smooth();
        
        return mb.build();
    }
    
    protected void addBranches( List<Vertex> base, Segment seg, 
                                float vBase, int uRepeat, float vScale, 
                                MeshBuilder mb, List<Vertex> tips ) {
 
        // Base the 'v' scale on what the 'u' will do as the tree expands
        // but the length doesn't.  ie: a ratio of length to radius.
        float vScaleLocal = vScale * (1 / seg.endRadius); 

        List<Vertex> tip = mb.extrude(base, seg.dir, seg.length, seg.radials, 
                                      seg.endRadius, seg.twist);       

        vBase += seg.length * vScaleLocal;
        
        mb.textureLoop(tip, new Vector2f(0, vBase), new Vector2f(uRepeat, 0));
        applyTangents(tip, seg.isInverted());

        if( !seg.hasChildren() ) {
            // Then cap it off by closing the loop.
 
            tip = mb.extrude(tip, seg.dir, 0, Vector3f.ZERO, 3, 0.001f, 0);
            mb.textureLoop(tip, new Vector2f(0, vBase + vScaleLocal), new Vector2f(uRepeat, 0));            
            applyTangents(tip, seg.isInverted());
            
            for( Vertex v : tip ) {
                v.group = 1;
            }
            
            // Find the center to add to the branch tips
            Vector3f centerPos = mb.findCenter(base);
            Vertex tipCenter = new Vertex(centerPos);
            tipCenter.normal = seg.dir;

            if( tips != null ) {
                tips.add(tipCenter);
            }            
            
            return;
        }

        // And the follow on segments
        for( Segment child : seg ) {
            switch( child.parentConnection ) {
                case Extrude:
                    // We can just continue directly
                    addBranches(tip, child, vBase, uRepeat, vScale, mb, tips);
                    break;
                case Abut:
                    throw new UnsupportedOperationException("Abutment not yet supported.");
                case Curve:
                
                    // This is the trickier one.
                    // We want a smooth transition from one direction to
                    // another, to include transition for any radius 
                    // changes that might be required.
                    Vector3f fromDir = seg.dir;
                    Vector3f toDir = child.dir;
                    float dot = fromDir.dot(toDir);
                    float tiltAngle = FastMath.acos(dot);

                    // How many corners will we have to make to not exceed
                    // some minimum angle.
                    float minAngle = (FastMath.DEG_TO_RAD * 15); 
                    int corners = (int)Math.ceil(tiltAngle / minAngle);
                    corners = Math.max(1, corners); // always at least one corner

                    // Calculate a minumum distance between slices
                    float radiusGap = Math.abs(seg.endRadius - child.startRadius);
                    float radiusGapStep = radiusGap / corners;
                    float minSlope = 5; // 5:1
                    float minDist = minSlope * radiusGapStep * dot; // dot is same as FastMath.cos(tiltAngle);
                    float vNextScale = vScale / child.startRadius;
                    float angleDelta = tiltAngle / corners;
                    float radiusPart = (seg.endRadius - child.startRadius) / corners;
                    float vScalePart = (vScale - vNextScale) / corners;        
 
                    Quaternion q1 = null;
                    Quaternion q2 = null;
                    if( dot != 1 ) {
                        // Calculate starting and engine quaternions representing
                        // the directions.
                        Vector3f left = fromDir.cross(toDir).normalizeLocal();
                        
                        q1 = new Quaternion().fromAxes(left, fromDir.cross(left), fromDir);
                        q2 = new Quaternion().fromAxes(left, toDir.cross(left), toDir);
                    } 
 
                    List<Vertex> newTip = tip;
                    float v = vBase; 
                    for( int i = 0; i < corners; i++ ) {
                        float a = (i + 1) * angleDelta;
                        float r = seg.endRadius - (radiusPart * (i+1));
                        float dist = seg.endRadius * FastMath.sin(angleDelta) * 1.4f; // magic number
                        dist = Math.max(dist, minDist);
                        Vector3f dir;
                        if( q1 == null ) {
                            dir = child.dir;
                        } else {
                            Quaternion q = new Quaternion().slerp(q1, q2, (float)(i+1)/corners);
                            dir = q.mult(Vector3f.UNIT_Z);
                        }
 
                        newTip = mb.extrude(newTip, dir, dist, seg.radials, r, 0);
                        v += dist * (vScaleLocal - (vScalePart * (i+1)));
                        mb.textureLoop(newTip, new Vector2f(0, v), new Vector2f(uRepeat, 0));
                        applyTangents(newTip, child.isInverted());                           
                    }                   
 
                    addBranches(newTip, child, v, uRepeat, vScale, mb, tips);
                
                    break;
            }
        }                
    }

    protected List<Vertex> invertLoop( List<Vertex> loop ) {
        List<Vertex> results = new ArrayList<Vertex>(loop);
        Collections.reverse(results);
        return results;
    }

    protected void applyTangents( List<Vertex> loop, boolean invert ) {
        
        for( int i = 1; i < loop.size(); i++ ) {
            Vertex last = loop.get(i-1);
            Vertex next = loop.get(i);
            
            Vector3f dir = next.pos.subtract(last.pos);
            dir.normalizeLocal();
            if( invert ) {
                dir.multLocal(-1);
            }
            last.tangent = dir;
        }
        
        // and match up the ends
        loop.get(loop.size()-1).tangent = loop.get(0).tangent;
    }
}
