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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 *
 *  @author    Paul Speed
 */
public class LodSwitchControl extends AbstractControl
                              implements Cloneable {

    private LodRange current = null;
    private List<LodRange> ranges = new ArrayList<LodRange>();
    
    private Camera camera = null;

    public LodSwitchControl() {
    }

    public LodSwitchControl clone() {
        try {
            LodSwitchControl c = (LodSwitchControl)super.clone();
            
            c.ranges = new ArrayList<LodRange>();
            for( LodRange r : ranges ) {
                c.ranges.add(r.clone());
            }           
            
            return c;
        } catch(CloneNotSupportedException e) {
            throw new RuntimeException( "Can't clone control", e );
        } 
    }

    public void addLevel( float range, Spatial child ) {
        child.setCullHint(CullHint.Always);
        ((Node)getSpatial()).attachChild(child);

        int childIndex = ((Node)getSpatial()).getChildIndex(child);

        LodRange r = new LodRange(range, childIndex);
        int index = Collections.binarySearch(ranges, r);
        if( index < 0 ) {
            index = -(index + 1); 
        } 
        ranges.add(index, r);
        
        // If there is a range already set after this one
        // then adjust its near range
        if( index + 1 < ranges.size() ) {
            LodRange next = ranges.get(index + 1);
            next.nearSq = range * range;
        }
        
        // If there is one before us then use that for
        // the near range
        if( index > 0 ) {
            LodRange previous = ranges.get(index - 1);
            r.nearSq = previous.farSq;
        }   
    }

    public void clearLevels() {
        ranges.clear();
        current = null;
    }
 
    public void removeLevel( Spatial child ) {
        int childIndex = ((Node)getSpatial()).getChildIndex(child);
 
        int removed = 0;
        for( Iterator<LodRange> it = ranges.iterator(); it.hasNext(); ) {
            LodRange r = it.next();
            if( r.child == childIndex ) {
                it.remove();
                child.removeFromParent();
                if( current != null && current.child == childIndex ) {
                    current = null;
                }
                removed++;
            } 
        }
 
        if( removed > 0 ) {       
            // Make sure the other ranges have their indexes adjusted
            // to account for the miss
            for( Iterator<LodRange> it = ranges.iterator(); it.hasNext(); ) {
                LodRange r = it.next();
                if( r.child > childIndex ) {
                    // Need to subtract to account for the removal
                    r.child -= removed;
                }
            }
        }
    }
 
    protected Spatial getChild( int index ) {
        return ((Node)getSpatial()).getChild(index);
    }
 
    protected final LodRange findLevel( float distSq ) {
        for( LodRange r : ranges ) {
            if( distSq >= r.nearSq && distSq <= r.farSq ) {
                return r;
            }    
        }
        return null;
    }

    protected final void resetLevel( float distSq ) {
        if( current != null && distSq >= current.nearSq && distSq <= current.farSq ) {
            // The current one is fine
            return;
        }
        if( current != null ) {
            getChild(current.child).setCullHint(CullHint.Always); 
        }
        current = findLevel(distSq);
        
        if( current != null ) {
            getChild(current.child).setCullHint(CullHint.Inherit);
        }       
    }

    protected float calculateDistance() {
        if( camera == null || getSpatial() == null ) {
            return 0;
        }
        float distanceSq = camera.getLocation().distanceSquared(getSpatial().getWorldTranslation());
        float scale = getSpatial().getWorldScale().x;
        if( scale != 1 ) {
            scale = 1 / scale;
            scale *= scale; // (d * s) * (d * s) = (d * d) * (s * s)
            distanceSq *= scale;
        } 
        return distanceSq; 
    }

    @Override
    protected void controlUpdate( float tpf ) {
        resetLevel(calculateDistance());
    }

    @Override
    protected void controlRender( RenderManager rm, ViewPort vp ) {
        if( camera == null ) {
            camera = vp.getCamera();
        }
    }
 
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList((ArrayList)ranges, "ranges", new ArrayList());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        ranges = (ArrayList<LodRange>)ic.readSavableArrayList("ranges", new ArrayList());
    }
 
    @Override
    public String toString() {
        return "LodSwitchControl[" + ranges + "]";
    }
    
    public static class LodRange implements Comparable<LodRange>, Savable {
        float nearSq;
        float farSq;
        int child;
        
        public LodRange( float range, int child ) {
            this.nearSq = 0;
            this.farSq = range * range;
            this.child = child;
        }
        
        public LodRange() {       
        }

        public LodRange clone() {
            LodRange result = new LodRange();
            result.nearSq = nearSq;
            result.farSq = farSq;
            result.child = child;
            return result;
        }

        @Override
        public int compareTo( LodRange t ) {
            return (int)(farSq - t.farSq);
        }

        @Override
        public void write( JmeExporter ex ) throws IOException {
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(nearSq, "nearSq", 0);
            oc.write(farSq, "farSq", 0);
            oc.write(child, "child", -1);
        }

        @Override
        public void read( JmeImporter im ) throws IOException {
            InputCapsule ic = im.getCapsule(this);
            this.nearSq = ic.readFloat("nearSq", 0);
            this.farSq = ic.readFloat("farSq", 0);
            //this.child = (Spatial)ic.readSavable("child", null);
            this.child = ic.readInt("child", -1);
        }
        
        @Override 
        public String toString() {
            return "LodRange[" + nearSq + ", " + farSq + ", " + child + "]";
        }
    }
}
