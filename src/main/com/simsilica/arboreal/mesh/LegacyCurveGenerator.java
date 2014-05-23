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
import com.jme3.math.Vector3f;
import java.util.Arrays;
import java.util.List;


/**
 *  The branch curve algorithm in use when I did the refactoring.
 *  It has some issues that I hope to fix in a different version,
 *  this the name.
 *
 *  @author    Paul Speed
 */
public class LegacyCurveGenerator implements CurveGenerator {

    private float minAngle = FastMath.DEG_TO_RAD * 15;
    private float minSlope = 5; // 5:1 slope 

    @Override
    public List<CurveStep> generateCurve( Vector3f startDir, float startRadius,
                                          Vector3f endDir, float endRadius,
                                          float v, float vScale ) {

        // Base the 'v' scale on what the 'u' will do as the tree expands
        // but the length doesn't.  ie: a ratio of length to radius.
        float vScaleLocal = vScale * startRadius; 
         
        // Calculate the total angle of change.  We will use
        // this to determine how many segments we'll need
        float dot = startDir.dot(endDir);
        float tiltAngle = FastMath.acos(dot);

        // How many corners will we have to make to not exceed
        // some minimum angle.
        int corners = (int)Math.ceil(tiltAngle / minAngle);
        corners = Math.max(1, corners); // always at least one corner

        CurveStep[] result = new CurveStep[corners];
        
        // Calculate a minumum distance between slices
        float radiusGap = Math.abs(endRadius - startRadius);
        float radiusGapStep = radiusGap / corners;
        float minDist = minSlope * radiusGapStep * dot; // dot is same as FastMath.cos(tiltAngle);

        float angleDelta = tiltAngle / corners;
        float radiusPart = (startRadius - endRadius) / corners;

        float vNextScale = vScale / endRadius;
        float vScalePart = (vScaleLocal - vNextScale) / corners;
        //float vScalePart = (vScale - vNextScale) / corners;
        //                  ^^^^ I think that's a bug.... going to refactor before testing        
 
        Quaternion q1 = null;
        Quaternion q2 = null;
        if( dot != 1 ) {
            // Calculate starting and ending quaternions representing
            // the directions.  We can then just interpolate between these.
            Vector3f left = startDir.cross(endDir).normalizeLocal();
                        
            q1 = new Quaternion().fromAxes(left, startDir.cross(left), startDir);
            q2 = new Quaternion().fromAxes(left, endDir.cross(left), endDir);
        } 
 
        Vector3f center = new Vector3f(0, 0, 0);
        
        for( int i = 0; i < corners; i++ ) {
            // Calculate the tapering of the radius
            float r = startRadius - (radiusPart * (i+1));
            
            // Calculate the angle at this corner so that we can
            // figure out a distance.  This is one of the parts that
            // needs to be fixed as it's kind of trial and error.
            float a = (i + 1) * angleDelta;
            float dist = startRadius * FastMath.sin(angleDelta) * 1.4f; // magic number                        
            dist = Math.max(dist, minDist);
            
            // Calculate the direction from the interpolated 
            // quaternion... if we aren't traveling in a straight line 
            Vector3f dir;
            if( q1 == null ) {
                dir = endDir.clone();
            } else {
                Quaternion q = new Quaternion().slerp(q1, q2, (float)(i+1)/corners);
                dir = q.mult(Vector3f.UNIT_Z);
            }

            v += dist * (vScaleLocal - (vScalePart * (i+1)));
 
            center = center.add(dir.mult(dist));           
            result[i] = new CurveStep(dir, dist, r, center, new Vector3f(), v);
        }
                                                       
        return Arrays.asList(result);                                              
    }                                              
}



