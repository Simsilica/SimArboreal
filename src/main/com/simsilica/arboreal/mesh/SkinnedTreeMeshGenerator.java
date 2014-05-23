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
import com.simsilica.arboreal.LevelOfDetailParameters;
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

    private CurveGenerator curveGen = CurveGenerator.DEFAULT;
    
    public Mesh generateMesh( Tree tree, LevelOfDetailParameters lod, float yOffset, int uRepeat, float vScale, List<Vertex> tips ) {
 
        MeshBuilder mb = new MeshBuilder();
 
        // Create the base loop for the main trunk... all
        // other segments branch off of that one.        
        Segment trunk = tree.getTrunk();
        
        Vector3f center = new Vector3f(0, yOffset, 0);

        int effectiveRadials = Math.min(trunk.radials, lod.maxRadialSegments);

        Quaternion up = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
        List<Vertex> baseLoop = mb.createLoop(center, up, trunk.startRadius, effectiveRadials, 0, 0);
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
                addBranches(invertedLoop, seg, 0, -uRepeat, -vScale, lod, 0, mb, null);
            } else {
                addBranches(baseLoop, seg, 0, uRepeat, vScale, lod, 0, mb, tips);
            }
        }
 
        mb.smooth();
        
        return mb.build();
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

    protected Vertex addCap( List<Vertex> loop, Segment seg, float vBase, int uRepeat, float vScaleLocal,
                             MeshBuilder mb ) {
        
        List<Vertex> tip = mb.extrude(loop, seg.dir, 0, Vector3f.ZERO, 3, 0.001f, 0);
        mb.textureLoop(tip, new Vector2f(0, vBase + vScaleLocal), new Vector2f(uRepeat, 0));            
        applyTangents(tip, seg.isInverted());
                
        for( Vertex v : tip ) {
            v.group = 1;
        }            
            
        // Find the center to add to the branch tips
        Vector3f centerPos = mb.findCenter(tip);
        Vertex tipCenter = new Vertex(centerPos);
        tipCenter.normal = seg.dir;
        
        return tipCenter;
    } 
 
    protected void addBranches( List<Vertex> base, Segment seg, 
                                float vBase, int uRepeat, float vScale,
                                LevelOfDetailParameters lod, int depth,  
                                MeshBuilder mb, List<Vertex> tips ) {
 
        // Base the 'v' scale on what the 'u' will do as the tree expands
        // but the length doesn't.  ie: a ratio of length to radius.
        float vScaleLocal = vScale * (1 / seg.endRadius); 

        int effectiveRadials = Math.min(seg.radials, lod.maxRadialSegments);

        boolean renderDepth = renderDepth(depth, seg.isInverted(), lod);
        
        List<Vertex> tip = base;                
        if( renderDepth ) {
            tip = mb.extrude(tip, seg.dir, seg.length, effectiveRadials, 
                             seg.endRadius, seg.twist);       

            vBase += seg.length * vScaleLocal;
        
            mb.textureLoop(tip, new Vector2f(0, vBase), new Vector2f(uRepeat, 0));
            applyTangents(tip, seg.isInverted());
        } else {
            // We still need to pass along the tip and/or cap off the end
            Vertex tipCenter;            
            if( tip.size() > 1 ) {
                // Cap it off
                tipCenter = addCap(tip, seg, vBase, uRepeat, vScaleLocal, mb);
                tip = new ArrayList<Vertex>();
                tip.add(tipCenter);            
            } else if( tip.size() == 1 ) {
                tipCenter = tip.get(0);
            } else {
                throw new IllegalStateException("Tip state not properly passed through");
            }
 
            // Extend the tip even though we don't render it.  We will
            // need the tips for the leaves.
            tipCenter.pos.addLocal(seg.dir.mult(seg.length));
            tipCenter.normal = seg.dir;            
            vBase += seg.length * vScaleLocal;
        }                   

        if( !seg.hasChildren() ) {
            // Then cap it off by closing the loop.
            Vertex tipCenter;
            if( renderDepth ) {
                tipCenter = addCap(tip, seg, vBase, uRepeat, vScaleLocal, mb);
            } else {
                if( tip.size() > 1 ) {
                    throw new IllegalStateException("Tip state not properly passed through");
                }
                tipCenter = tip.get(0);
            }

            if( tips != null ) {
                tips.add(tipCenter);
            }            
            
            return;
        }

        boolean renderNextDepth = renderDepth;
        boolean capped = tip.size() == 1;
        if( !renderDepth(depth + 1, seg.isInverted(), lod) ) {
            renderNextDepth = false;             
        }                    

        // And the follow on segments
        for( Segment child : seg ) {
            switch( child.parentConnection ) {
                case Extrude:
                    // We can just continue directly
                    addBranches(tip, child, vBase, uRepeat, vScale, lod, depth, mb, tips);
                    break;
                case Abut:
                    throw new UnsupportedOperationException("Abutment not yet supported.");
                case Curve:
 
                    List<Vertex> newTip = tip;
                    float v = 0;
                    
                    if( !renderNextDepth ) {
                        if( !capped ) {
                            // Cap the previous level off... but only for the first child do we need to
                            capped = true;
                            Vertex tipCenter = addCap(tip, seg, vBase, uRepeat, vScaleLocal, mb);
                            tip = new ArrayList<Vertex>();
                            tip.add(tipCenter);
                        } else if( newTip.size() != 1 ) {
                            // check should be unnecessary
                            throw new IllegalStateException("Tip state not properly passed through");
                        }
                         
                        // Make sure this branch has its own tip to move
                        Vertex tipCenter = newTip.get(0).clone();
                        newTip = new ArrayList<Vertex>();
                        newTip.add(tipCenter);
                    }
                
                    List<CurveStep> steps = curveGen.generateCurve(seg.dir, seg.endRadius,
                                                                   child.dir, child.startRadius,
                                                                   vBase, vScale);
 
                    if( renderNextDepth ) {
                        v = 0;                                                                  
                        for( CurveStep step : steps ) {
                            v = step.v;
                            if( renderNextDepth ) {
                                newTip = mb.extrude(newTip, step.dir, step.distance, step.offset,
                                                    effectiveRadials, step.radius, 0);
                                mb.textureLoop(newTip, new Vector2f(0, step.v), new Vector2f(uRepeat, 0));
                                applyTangents(newTip, child.isInverted());
                            } 
                        }
                    } else {
                        // Just advance the tip to the end
                        if( newTip.size() != 1 ) {
                            throw new IllegalStateException("Tip state not properly passed through");
                        }
                        // Extend the tip
                        CurveStep step = steps.get(steps.size() - 1);
                        Vertex tipCenter = newTip.get(0);
                        tipCenter.pos.addLocal(step.center);                            
                        tipCenter.normal = step.dir;
                        v = step.v;
                    }
                    
                    addBranches(newTip, child, v, uRepeat, vScale, lod, depth + 1, mb, tips);
                
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
