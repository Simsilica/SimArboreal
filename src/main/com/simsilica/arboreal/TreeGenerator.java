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

package com.simsilica.arboreal;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.arboreal.Segment.ConnectionType;
import java.util.List;
import java.util.Random;


/**
 *  Generates a tree from a set of TreeParameters
 *  describing the inputs to a simplified parameterized
 *  L-system.
 *
 *  @author    Paul Speed
 */
public class TreeGenerator {

    public Tree generateTree( TreeParameters treeParms ) {
        return generateTree(treeParms.getTrunkRadius(), treeParms.getTrunkHeight(),
                            treeParms.getRootHeight(), treeParms.getSeed(), treeParms);
    }

    public Tree generateTree( int seed, TreeParameters treeParms ) {
        return generateTree(treeParms.getTrunkRadius(), treeParms.getTrunkHeight(),
                            treeParms.getRootHeight(), seed, treeParms);
    }
    public Tree generateTree( float radius, float trunkHeight, float rootHeight, int seed,
                              TreeParameters treeParms ) {
 
        Tree result = new Tree();
 
        Random random = new Random(seed);
        
        // Straight up rotation for the trunk
        Quaternion rotation = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
        
        float length = trunkHeight;
        float lengthOffset = rootHeight;
        
        Segment trunk = createBranch(random, 0, treeParms.getEffectiveBranches(),
                                     rotation, radius, length, lengthOffset,
                                     0, 0, 
                                     treeParms.getTextureURepeat(), 
                                     treeParms.getTextureVScale());
        result.setTrunk(trunk);
 
        rotation = new Quaternion().fromAngles(FastMath.HALF_PI, 0, 0);
        Segment roots = createBranch(random, 0, treeParms.getEffectiveRoots(),
                                     rotation, radius, rootHeight, 0, 
                                     0, 0, 
                                     treeParms.getTextureURepeat(),  
                                     -treeParms.getTextureVScale());
        result.setRoots(roots);
                              
        return result;                              
    } 
 
    protected Segment createBranch( Random random, int depth, List<BranchParameters> bParms,
                                    Quaternion rotation, float radius, 
                                    float length, float lengthOffset,
                                    float baseAngle,
                                    float vBase, int uRepeat, float vScaleTree ) {
 
        if( depth >= bParms.size() ) {
            throw new IllegalArgumentException("Depth exceeds parameters.");
        }

        Segment result = new Segment();
        result.dir = rotation.mult(Vector3f.UNIT_Z);
        result.startRadius = radius;
        result.uScale = uRepeat;
        result.vStart = vBase;        
 
        BranchParameters parms = bParms.get(depth);
 
        // Add the segments for this branch, factoring in 
        // variation and gravity as we go.
        //----------------------------------------------------

        // Base the 'v' scale on what the 'u' will do as the tree expands
        // but the length doesn't.  ie: a ratio of length to radius.
        // Note: vScale will be negative for branches growing down from the
        //       base of the tree, ie: tree roots.
        float vScale = vScaleTree * (1 / radius);
 
        // Calculate an unnormalized variation.  note: 3.14 is a coincidence
        // and was found through experimentation.  It's not really PI.       
        float variation = parms.segmentVariation; 
        variation = variation * variation;
        variation *= 3.14f;
         
        // Gravity up until now is -1 to 1.  We move it into an unnormalized
        // range that was also found through experimentation.  Essentially
        // this says that a branch pointing straight up can almost but not
        // quite do a full 180.
        float maxGravity = FastMath.HALF_PI * 1.95f;
        float effectiveGravity = parms.gravity * maxGravity;
 
        // lengthOffset subtracts from length but we need to make sure
        // it is never less than 0.  It's a way of forcing the children
        // to have a nice parent length even if this branch is hardly
        // rendered.
        float effectiveLength = Math.max(0, length - lengthOffset);
        float effectiveTaper = parms.taper;
        
        // If we are going down instead of up (for roots) then invert
        // the taper the first 'trunk'
        if( vScale < 0 && depth == 0 ) {
            // So, if the taper were 0.75 then we'd want to continue
            // expanding such that we maintain that 0.75.
            // x * 0.75 = 1
            // x = 1 / 0.75
            effectiveTaper = 1 / parms.taper;
        }
        // Note: because we are upside down, the taper is already 
        // reversed.
        
        Quaternion originalRotation = rotation; 
        
        Segment tip;
        if( effectiveLength <= 0 ) {
            result.length = 0;
            result.endRadius = result.startRadius;
            result.vEnd = result.vStart;
            tip = result;
        } else {
            // Divide things up into their per-segment parts
            float lengthPart = effectiveLength / parms.lengthSegments;
            float taperPart = (1 - effectiveTaper) / parms.lengthSegments;
            float twistPart = parms.twist / parms.lengthSegments;  
            float gravityPart = effectiveGravity / parms.lengthSegments;      
        
            // We need to limit variation based on the segment lengths
            // versus the radius... we'll adjust the angle limit accordingly
            // Essentially, we want to make sure a typical bend doesn't bend
            // over itself.  If length == radius, for example, the 30 is the
            // max degrees and then the edges are touching.
            // ...and even then we'd want half of that just to be sure we
            // didn't collide with the last bend.
            // So, if we imagine a triangle with radius as the base, lengthPart
            // as one side, and radius as the other side, we have two similar 
            // triangles split down the middle.
            // sin = (lengthPart / 2) / radius... or
            // sin = lengthPart / (2 * radius)
            // The full angle between the radii would be 2 sin but we already
            // only want half.
            float angleSin = lengthPart / (2 * (radius * effectiveTaper));        
            float angleLimit = FastMath.asin(angleSin);
            angleLimit = Math.min(angleLimit, FastMath.HALF_PI * 0.33f * 0.5f);
 
            tip = result;
            
            for( int i = 0; i < parms.lengthSegments; i++ ) {
                int index = i + 1;
                
                Vector3f dir = tip.dir;
            
                // Figure out what "world down" is in branch space.  We need
                // to know which direction to apply a gravity rotation
                Vector3f down = originalRotation.inverse().mult(new Vector3f(0, -1, 0));
                
                // Now project that against or branch direction... which will
                // tell us how orthogonal to gravity we are.  (Straight up
                // or straight down will have no gravity influence, straight to
                // the side will have maximum gravity influence.)
                float downAmount = 1 - Math.abs(Vector3f.UNIT_Z.dot(down));                  
                if( downAmount != 0 ) {
                    // Guaranteed to get a clean cross product because we already
                    // know down and direction are not parallel. (see above)
                    Vector3f side = Vector3f.UNIT_Z.cross(down).normalize();
                    
                    // So, figure out the rotation around this hinge and
                    // compose it with our current rotation.
                    Quaternion gravRot = new Quaternion().fromAngleAxis(gravityPart * downAmount, side);
                    rotation = originalRotation.mult(gravRot);
                    originalRotation = rotation;
                    dir = rotation.mult(Vector3f.UNIT_Z);
                }
                                 
                if( variation != 0 ) {
                    // Going to do variation in angles + length... kind of a 3D polar
                    // system.
                    float x = random.nextFloat() * (variation * 2) - variation;
                    float y = random.nextFloat() * (variation * 2) - variation;
                    float z = random.nextFloat() * (variation * 2) - variation;
                    
                    // Note: z is unused but I'm keeping it for parity with
                    // existing random trees, ie: keeping the random sequence
                    // similar.
 
                    // Clamp our polar coordinates to the angle limit
                    x = FastMath.clamp(x, -angleLimit, angleLimit);
                    y = FastMath.clamp(y, -angleLimit, angleLimit);
                
                    // Apply the variation rotation but don't replace the
                    // original rotation.  This keeps the tree from randomly
                    // sprawling in odd directions as the whole branch will
                    // tend to go in the original direction.    
                    Quaternion rot = new Quaternion().fromAngles(y, x, 0);
                    rotation = originalRotation.mult(rot);
                    tip.dir.set(rotation.mult(Vector3f.UNIT_Z));
                }
                
                // So now extend the previous segment
                tip.endRadius = radius * (1 - taperPart * index);
                tip.vEnd = vBase + (index * lengthPart) * vScale;
                tip.length = lengthPart;
                tip.radials = parms.radialSegments;
                tip.twist = twistPart;
                
                // If there will be more parts then we get a new tip
                if( i + 1 < parms.lengthSegments ) {
                    tip = tip.extend(ConnectionType.Extrude, true);
                } 
            }
        }
 
        if( depth + 1 >= bParms.size() ) {
            // We will not be branching any further so we can return
            // early
            return result;
        }
 
        
        // Add the child branches
        //-----------------------------------------
 
        // A slightly questionable policy... remove any randomization
        // for the branch joints.  This also keeps trees from leaning 
        // strangely thought it's possible that other angleLimiting
        // additions have made this less critical.  I'm going for
        // parity with the prototype for now so that I can easily
        // compare and find regressions.       
        rotation = originalRotation;
        
        // Bring vBase up to speed
        vBase += effectiveLength * vScale;
        
        // Grab inclination and put it in an appropriate form.
        // "inclination" is inverted from what we need as
        // tilteAngle will be relative to branch direction and
        // inclination is relative to an orthogonal plane.
        float tiltAngle = FastMath.HALF_PI - parms.inclination;
                
        // Calculate the next level base branch radius based on the
        // current radius divided into sections... we use the circle
        // area because, well, that's kind of how trees work.  But if
        // we only have one branch then we pretend there are two.
        float rootArea = FastMath.PI * radius * radius;
        float branchArea = rootArea / Math.min(2, parms.sideJointCount); 
        float baseBranchRadius = (float)Math.sqrt(branchArea / FastMath.PI); 
        float branchRadius = parms.radiusScale * baseBranchRadius; 
        float branchLength = parms.lengthScale * length;
 

        float startAngle = parms.sideJointStartAngle + parms.twist + baseAngle;
        float jointAngleDelta = FastMath.TWO_PI / parms.sideJointCount;

        // Prep the tip for its new children
        int childCount = parms.sideJointCount + (parms.hasEndJoint ? 1 : 0); 
        tip.children = new Segment[childCount];
 
        for( int b = 0; b < parms.sideJointCount; b++ ) {
            float jointAngle = startAngle + jointAngleDelta * b;
            
            Quaternion angleRotation = new Quaternion().fromAngles(0, 0, jointAngle);
            Quaternion tiltRotation = new Quaternion().fromAngles(tiltAngle, 0, 0);
            Quaternion branchRotation = rotation.mult(angleRotation).mult(tiltRotation);
            
            tip.children[b] = createBranch(random, depth+1, bParms, 
                                           branchRotation, branchRadius,
                                           branchLength, 0, 0,
                                           vBase, uRepeat, vScaleTree);              
        }         
 
        // Add the tip recursion
        //-------------------------------       
        if( parms.hasEndJoint ) {
            tip.children[parms.sideJointCount] = createBranch(random, depth+1, bParms, 
                                                              rotation, radius, length * parms.taper,
                                                              0, baseAngle + parms.twist + parms.tipRotation,
                                                              vBase, uRepeat, vScaleTree); 
        }
                                                        
        return result;                                    
    }                                         
                                        
}


