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
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A utility for building up a mesh from triangles, loops, and
 *  extrusions, including automatic calculation of smooth normals,
 *  vertex groups, and so on.
 *
 *  @author    Paul Speed
 */
public class MeshBuilder {

    static Logger log = LoggerFactory.getLogger(MeshBuilder.class);

    public static final float DEFAULT_EPSILON = 0.001f;
    public static final float EXACT_EPSILON = 0;

    private List<Vertex> verts = new ArrayList<Vertex>();
    private List<Triangle> triangles = new ArrayList<Triangle>();
    private Map<Vertex, NormalLinks> linksMap = new HashMap<Vertex, NormalLinks>();
    private List<NormalLinks> links = new ArrayList<NormalLinks>(); 
    
    public MeshBuilder() {
    }
 
    protected Vertex newVertex( Vector3f v, int group ) {
        return newVertex(v.x, v.y, v.z, group);
    }

    protected Vertex newVertex( float x, float y, float z, int group ) {
        Vertex result = new Vertex(x, y, z);
        result.index = verts.size();
        result.group = group;
        verts.add(result);
        return result;
    }

    protected Vertex newVertex( float x, float y, float z, float u, float v, int group ) {
        Vertex result = newVertex(x, y, z, group);
        result.uv = new Vector2f(u, v);
        return result;
    }
    
    public Vertex createVertex( Vector3f v, float epsilon ) {
        return createVertex(v.x, v.y, v.z, 0, epsilon);
    }

    public Vertex createVertex( float x, float y, float z, float epsilon ) {
        return createVertex(x, y, z, 0, epsilon);
    }

    public Vertex createVertex( float x, float y, float z ) {
        return createVertex(x, y, z, 0, DEFAULT_EPSILON);
    }
    
    public Vertex createVertex( float x, float y, float z, int group ) {
        return createVertex(x, y, z, group, DEFAULT_EPSILON);
    }
    
    public Vertex createVertex( float x, float y, float z, int group, float epsilon ) {
        // See if we've already got one
        for( Vertex vert : verts ) {
            if( group >= 0 && vert.group != group ) {
                continue;
            }
            if( vert.isSame(x, y, z, epsilon) ) {
                return vert;
            }
        }
        return newVertex(x, y, z, group);
    }

    public Vertex createVertex( float x, float y, float z, float u, float v, int group ) {
        return createVertex(x, y, z, u, v, group, DEFAULT_EPSILON);
    }
    
    public Vertex createVertex( float x, float y, float z, float u, float v, int group, float epsilon ) {
        // See if we've already got one
        for( Vertex vert : verts ) {
            if( group >= 0 && vert.group != group ) {
                continue;
            }
            if( vert.isSame(x, y, z, u, v, epsilon) ) {
                return vert;
            }
        }
        return newVertex(x, y, z, u, v, group);
    }

    public void addTriangle( Vertex v1, Vertex v2, Vertex v3 ) {
        triangles.add(new Triangle(v1, v2, v3));
    }
 
    /**
     *  Links two vertexes together such that they will
     *  share the same smooth normal in smoothing calculations.
     */
    public void linkNormals( Vertex v1, Vertex v2 ) {
        NormalLinks nl1 = linksMap.get(v1);
        NormalLinks nl2 = linksMap.get(v2);
        if( nl1 == nl2 && nl1 != null ) {
            return;  // already linked
        }
        
        if( nl1 != null && nl2 != null ) {
            // Need to merge them
            nl1.set.addAll(nl2.set);
            
            // Then let all of the nl2 links know they have a new
            // links set.
            for( Vertex v : nl2.set ) {
                linksMap.put(v, nl1);
            }
            
            // Remove the old nl2 from the main list
            links.remove(nl2);
            return;            
        }
        
        if( nl1 != null ) {
            // v2 is the new one
            nl1.set.add(v2);
            linksMap.put(v2, nl1);
        } else if( nl2 != null ) {
            // v1 is the new one
            nl2.set.add(v1);
            linksMap.put(v1, nl2);
        } else {
            // Never linked before
            NormalLinks nl = new NormalLinks();
            nl.set.add(v1);
            nl.set.add(v2);
            linksMap.put(v1, nl);
            linksMap.put(v2, nl);
            links.add(nl);
        }
    }
 
    /**
     *  Smooths the vertex normals by creating a weighted average
     *  of the triangle normals shared by a particular vertex or
     *  linked vertex.  The weighting is calculated based on the
     *  angle between adjacent edges.
     */
    public void smooth() {
        for( Vertex vert : verts ) {
            if( vert.weight != -1 ) {
                vert.weight = 0;
            }
        }
        
        for( Triangle tri : triangles ) {
            Vector3f normal = tri.calculateNormal();
 
            for( Vertex vert : tri.vertexes() ) {
                if( vert.weight == -1 ) {
                    continue;
                }                
                // The normal is weighted based on the angle
                // between the adjacent edges of the vertex for
                // this particular triangle.  The theory is that
                // for a continuous surface, a vertex in the center's
                // total 'weight' would then add up to 360 degrees
                // and each triangle then contributes its circular
                // portion of 'normal' for that vertex.  It seems to
                // work in practice.                
                float weight = tri.angle(vert); 
                if( vert.normal == null ) {
                    vert.normal = normal.mult(weight);
                } else {
                    vert.normal.addLocal(normal.mult(weight));
                }
                vert.weight += weight;                 
            }        
        }
        
        // Combine any linkages
        for( NormalLinks nl : links ) {
            nl.combineNormals();
        }
        
        // Now average them
        for( Vertex vert : verts ) {
            if( vert.normal == null || vert.weight <= 0 ) {
                continue;
            }
            vert.normal.mult(1f/vert.weight); 
            vert.normal.normalizeLocal();
        }
    }
 
    
    /**
     *  Connects two vertex loops together by intermediate triangles.
     *  Each loop is assumed to have an extra joining vertex.  It
     *  as also assumed that each loop is already aligned such that
     *  it is acceptible to connect vertex[0] or one loop with 
     *  vertex[0] of the second as the starting edge.
     */
    public void connect( List<Vertex> loop1, List<Vertex> loop2 ) {
        if( loop1.isEmpty() || loop2.isEmpty() ) {
            throw new IllegalArgumentException("Loops cannot be empty.");
        }
 
        boolean sameSize = loop1.size() == loop2.size();
        
        int i = 0; 
        int j = 0;
        // Keep going until we've used all the vertexes from
        // both loops.
        while( i < loop1.size() && j < loop2.size() ) {
            Vertex last1 = loop1.get(i);
            Vertex last2 = loop2.get(j);
            Vertex next1 = i < loop1.size() - 1 ? loop1.get(i + 1) : null;   
            Vertex next2 = j < loop2.size() - 1 ? loop2.get(j + 1) : null;
            
            if( log.isTraceEnabled() ) {
                log.trace("last:" + last1.pos + "  " + last2.pos);
                log.trace("next:" + (next1 == null  ? "null" : next1.pos.toString())
                                  + "  " + (next2 == null ? "null" : next2.pos.toString()));
            }

            if( next1 == null && next2 == null ) {
                log.trace("loops complete");
                // We're done
                break;
            }

            // Need to pick the best canidate.  The best
            // candidate is the one that best maintains the 'convex'
            // nature of the two loops.  We could make the two edges
            // and see which was on top where they cross.
            // right now I'm going to go with "shortest".
            
            // But either way, if we are at the end of one loop or
            // the other then the candidate is already chosen
            Vertex next;
            if( next1 == null ) {
                log.trace("At end of loop1.");
                next = next2;
                j++;            
            } else if( next2 == null ) {
                log.trace("At end of loop2.");
                next = next1;
                i++;
            } else { 
                // Figure out the distances across                
                float dist1;
                float dist2;
                if( sameSize ) {
                    // Force same size loops to go with stricter ordering.
                    // It's kind of a cop-out because the distance test doesn't
                    // work very well when both loops are at strange angles
                    // We could instead use radians or something or a more 
                    // expensive calculation but I want to move on
                    dist1 = j;
                    dist2 = i;
                } else {
                    dist1 = last1.pos.distanceSquared(next2.pos);
                    dist2 = last2.pos.distanceSquared(next1.pos);
                }
                if( log.isTraceEnabled() ) {
                    log.trace("last1 to next2:" + dist1 + "  last2 to next1:" + dist2);
                }
                
                if( dist1 < dist2 ) {
                    log.trace("loop2 next is closer." );                
                    next = next2;
                    j++;
                } else {
                    log.trace("loop1 next is closer." );                
                    next = next1;
                    i++;
                }
            } 

            if( log.isTraceEnabled() ) {
                log.trace("+Triangle(" + last2.pos + ", " + last1.pos + ", " + next.pos + ")");                       
            }
            addTriangle(last2, last1, next);
        }
    }
 
    /**
     *  Sets the texture coordinates of a vertex loop using
     *  base and range.  The range parameter is used to specify
     *  how either u or v will change around the loop.  The base
     *  is what each loop value will be added to.
     *  For example: 
     *     base = 0, 0, range = 1, 0
     *  will apply a uv of 0, 0 to 1.0, 0 over the whole loop... with the
     *  start vertex getting 0, 0 and the end vertex getting 1, 0.
     *  Alternately, base = 1, 1, range = 0, 1
     *  will apply a uv of 1, 1 to the start vertex and interpolate through
     *  1, 2 at the last vertex.
     */
    public void textureLoop( List<Vertex> loop, Vector2f base, Vector2f range ) {
        
        int count = loop.size();
        float uDelta = range.x / count;
        float vDelta = range.y / count;
        float xBase = base.x;
        if( uDelta < 0 ) {
            xBase = (range.x * -1) + uDelta;
        }
        
        for( int i = 0; i < count; i++ ) {
            float u = xBase + i * uDelta;
            float v = base.y + i * vDelta;
            Vertex vert = loop.get(i);
            if( vert.uv == null ) {
                vert.uv = new Vector2f(u, v);
            } else {
                vert.uv.set(u, v);
            }
        }
    }

    /**
     *  Finds the geometric center of a vertex loop.
     */
    public Vector3f findCenter( List<Vertex> loop ) {
        Vector3f center = new Vector3f();
        // Need to skip the last one because it's redundant
        // and will mess up the average
        int count = loop.size() - 1;
        for( int i = 0; i < count; i++ ) {
            Vertex vert = loop.get(i);
            center.addLocal(vert.pos);
        }
        float scale = 1f/count;
        center.multLocal(scale);
        return center;    
    }
                                 
    /**
     *  Extrudes a vertex loop out to another vertex loop, connecting
     *  both loops, and returning the new extruded loop.  The dir and
     *  distance parameters define the new loop's center relative to
     *  the specified loop's center.  The segments paramter specifies
     *  the number of vertexes in the new loop (segments + 1).  The
     *  radius and twist specify the size and starting angle of the
     *  new loop.
     */    
    public List<Vertex> extrude( List<Vertex> loop, Vector3f dir, float distance, 
                                 int segments, float radius, float twist ) {
        return extrude(loop, dir, distance, null, segments, radius, twist);                                 
    }                                 

    /**
     *  Extrudes a vertex loop out to another vertex loop, connecting
     *  both loops, and returning the new extruded loop.  The dir and
     *  distance parameters define the new loop's center relative to
     *  the specified loop's center.  The segments paramter specifies
     *  the number of vertexes in the new loop (segments + 1).  The
     *  radius and twist specify the size and starting angle of the
     *  new loop.  The specified offset is applied _after_ loop
     *  extrusion and connection to give the new loop the best chance
     *  at properly connecting with the old loop.
     */    
    public List<Vertex> extrude( List<Vertex> loop, Vector3f dir, float distance, Vector3f offset, 
                                 int segments, float radius, float twist ) {
                                 
        Vector3f center = findCenter(loop);
        Vertex first = loop.get(0);
        
        if( log.isTraceEnabled() ) {
            log.trace("Extrusion center point:" + center );                         
        }

        // Calculate the new loop's pre-offset center
        Vector3f base = center.add(dir.mult(distance)); 

        // The best generate a new loop in similar orientation
        // to the original, we construct a set of axes in 'loop space'
        // where creating a vertex at angle 0 will likely create the
        // best alignment.
        // The z axis will be the new loop's normal.  y and x axes
        // are determined based on the projection of loop1's first
        // point.        
        Vector3f look = dir;
        Vector3f right = first.pos.subtract(center).normalizeLocal();
        Vector3f left = right.mult(-1); 
        Vector3f up = look.cross(left).normalizeLocal();
        Quaternion loopRotation = new Quaternion().fromAxes(left, up, look);
        Vector3f[] axes = new Vector3f[3];
        loopRotation.toAxes(axes);
        
        if( log.isTraceEnabled() ) {
            log.trace("axes x:" + axes[0] + " y:" + axes[1] + " z:" + axes[2]);
        }

        // Because UNIT_X would point left... and we start from the right.                
        float originalTwist = FastMath.PI;  
 
        // Create the new loop
        List<Vertex> newLoop = createLoop(base, loopRotation, radius, segments, twist + originalTwist, 0);
 
        // Connect it
        connect(loop, newLoop);
 
        if( offset != null ) {
            // Offset the new loop post creation. (Note: loops are always created
            // from new vertexes which prevents auto-joining to existing structures.
            // this is a really good thing here as offseting would totally mess up
            // autojoined vertexes.) 
            // It's important to do this after creation to give the connect() call the best
            // chance of doing the right thing.
            for( Vertex v : newLoop ) {
                v.pos.addLocal(offset);
            }
        }
                                 
        return newLoop;
    }
 
    /**
     *  Creates a safe lookAt() quaternion even if dir and the
     *  default up happen to point in the same direction.
     */
    protected Quaternion lookAt( Vector3f dir ) {
        Quaternion rot = new Quaternion();
        if( Math.abs(dir.y) == 1.0 ) {
            rot.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_Z);
        } else {
            rot.lookAt(dir, Vector3f.UNIT_Y);
        }
        return rot;
    }
 
    /**
     *  Creates a new vertex loop with the specified center, axis, and radius.
     *  The axis acts as a 'loop normal' and twist determines how much additional
     *  'angle' to give the first vertex.
     */
    public List<Vertex> createLoop( Vector3f center, Vector3f axis, float radius,
                                    int segments, float twist, int group ) {
        Quaternion loopRotation = lookAt(axis);
        return createLoop(center, loopRotation, radius, segments, twist, group);                                    
    }
    
    /**
     *  Creates a new vertex loop with the specified center, orientation, and radius.
     *  Orientation controls where the first vertex will be as well as the loop normal.
     *  Vertexes are created in 'loop space' as determined by the center and orientation.
     *  For example, angle 0 will be down the X-axis after orientation transformation.
     */
    public List<Vertex> createLoop( Vector3f center, Quaternion orientation, float radius,
                                    int segments, float twist, int group ) {
 
        List<Vertex> newLoop = new ArrayList<Vertex>();
        float angleDelta = FastMath.TWO_PI / segments;
        Quaternion local = new Quaternion();
        Vector3f pos = new Vector3f();
        for( int i = 0; i <= segments; i++ ) {
            // Combine this radials angle with the base orientation
            float a = i * angleDelta;
            local.fromAngles(0, 0, a + twist);
            local = orientation.mult(local, local);
            
            // Extend the vertex down the x-axis of this combined
            // rotation.
            pos.set(radius, 0, 0);
            pos = local.multLocal(pos);
            pos.addLocal(center);           
            newLoop.add(newVertex(pos.x, pos.y, pos.z, 0));
        }

        // Link the normals of the first and last vertex so that
        // there won't be a seam.
        linkNormals(newLoop.get(0), newLoop.get(segments));                                    

        return newLoop;
    }

    public Mesh build() {
        if( verts.isEmpty() || triangles.isEmpty() ) {
            return null;
        }

        if( log.isInfoEnabled() ) {
            log.info("Creating a mesh with:" + verts.size() + " vertexes and:" + triangles.size() + " triangles.");
        }
        
        Mesh mesh = new Mesh();
 
        boolean hasNormals = verts.get(0).normal != null;
        boolean hasUvs = verts.get(0).uv != null;
        boolean hasTangents = verts.get(0).tangent != null;
        
        FloatBuffer pb = BufferUtils.createFloatBuffer(verts.size() * 3);
        FloatBuffer nb = null;
        if( hasNormals ) {
            nb = BufferUtils.createFloatBuffer(verts.size() * 3); 
        }
        FloatBuffer tb = null;
        if( hasUvs ) {
            tb = BufferUtils.createFloatBuffer(verts.size() * 2); 
        }
        FloatBuffer tanb = null;
        if( hasTangents ) {
            tanb = BufferUtils.createFloatBuffer(verts.size() * 4); 
        }

        for( Vertex vert : verts ) {
            Vector3f v = vert.pos;
            pb.put(v.x).put(v.y).put(v.z);
            if( nb != null ) {
                Vector3f n = vert.normal;
                nb.put(n.x).put(n.y).put(n.z);
            }
            if( tb != null ) {
                Vector2f uv = vert.uv != null ? vert.uv : Vector2f.ZERO;
                tb.put(uv.x).put(uv.y);
            }
            if( tanb != null ) {
                Vector3f t = vert.tangent;
                tanb.put(t.x).put(t.y).put(t.z).put(1);
            }
        }

        mesh.setBuffer(Type.Position, 3, pb);
        if( nb != null ) {
            mesh.setBuffer(Type.Normal, 3, nb);
        }
        if( tb != null ) {
            mesh.setBuffer(Type.TexCoord, 2, tb);
        }
        if( tanb != null ) {
            mesh.setBuffer(Type.Tangent, 4, tanb);
        }

        // Now the index buffer
        if( verts.size() <= 0xffff ) {
            ShortBuffer ib = BufferUtils.createShortBuffer(triangles.size() * 3);
            for( Triangle tri : triangles ) {
                ib.put((short)tri.v1.index);
                ib.put((short)tri.v2.index);
                ib.put((short)tri.v3.index);
            }
            mesh.setBuffer(Type.Index, 3, ib);
        } else {
            IntBuffer ib = BufferUtils.createIntBuffer(triangles.size() * 3);
            for( Triangle tri : triangles ) {
                ib.put(tri.v1.index);
                ib.put(tri.v2.index);
                ib.put(tri.v3.index);
            }
            mesh.setBuffer(Type.Index, 3, ib);
        }
        
        mesh.updateBound();                                
        return mesh;        
    }
    
    private class NormalLinks {
        Set<Vertex> set = new HashSet<Vertex>();
        
        void combineNormals() {
            Vector3f normal = new Vector3f();
            float total = 0;
            
            // Make a pass to accumulate the combined normal and total
            for( Vertex v : set ) {
                normal.addLocal(v.normal);
                total += v.weight;
            }
            
            // Then apply them back
            for( Vertex v : set ) {
                v.normal.set(normal);
                v.weight = total;
            }
        }
    } 
}
