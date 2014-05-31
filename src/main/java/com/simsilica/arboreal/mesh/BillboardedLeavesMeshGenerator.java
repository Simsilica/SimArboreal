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

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;


/**
 *
 *  @author    Paul Speed
 */
public class BillboardedLeavesMeshGenerator {

    public Mesh generateMesh( List<Vertex> locations, float quadSize ) {
 
System.out.println( "Quad size:" + quadSize );
    
        // Generate quads, four points each, where each corner
        // has the same model position.
        // Corner information is encoded into the texture coordinate.
        Vector3f[] posArray = new Vector3f[locations.size() * 4];
        Vector3f[] normArray = new Vector3f[locations.size() * 4];
        Vector4f[] uvArray = new Vector4f[locations.size() * 4];
        float[] sizeArray = new float[locations.size() * 4];
        short[] indexArray = new short[locations.size() * 2 * 3];
        int index = 0;
        int baseIndex = 0;
        
        // Could have done random coordinates or something but
        // I'm just assigning them sequentially.
        int textureCellIndex = 0;
        int uCells = 1;
        float uCellSize = 1f / 4; // uCells;  we only use one column but the atlas really has 4
        int vCells = 4;
        float vCellSize = 1f / vCells;
        
        for( Vertex v : locations ) {
            Vector3f p = v.pos;

            posArray[baseIndex+0] = p; 
            posArray[baseIndex+1] = p; 
            posArray[baseIndex+2] = p; 
            posArray[baseIndex+3] = p;

            int vCell = (textureCellIndex % (vCells * 2)); 
            int uCell = ((textureCellIndex / (vCells * 2)) % (uCells * 2));
            
            float uBase;
            float uTop;
            if( uCell < uCells ) {
                // Do negative cells
                uTop = -1.0f + uCell * uCellSize;
                uBase = uTop + uCellSize;
            } else {
                // Do positive cells
                uBase = (uCell - uCells) * uCellSize;
                uTop = uBase + uCellSize;
            }

            float vBase;
            float vTop;
            if( vCell < vCells ) {
                // Do negative cells
                vTop = -1.0f + vCell * vCellSize;
                vBase = vTop + vCellSize;
            } else {
                // Do positive cells
                vBase = (vCell - vCells) * vCellSize;
                vTop = vBase + vCellSize;
            }
             
            uvArray[baseIndex+0] = new Vector4f(0, 0, uBase, vBase);            
            uvArray[baseIndex+1] = new Vector4f(1, 0, uTop, vBase);            
            uvArray[baseIndex+2] = new Vector4f(1, 1, uTop, vTop);            
            uvArray[baseIndex+3] = new Vector4f(0, 1, uBase, vTop);
                
            textureCellIndex++;
 

            normArray[baseIndex+0] = v.normal;
            normArray[baseIndex+1] = v.normal;
            normArray[baseIndex+2] = v.normal;
            normArray[baseIndex+3] = v.normal;
 
            sizeArray[baseIndex+0] = quadSize;
            sizeArray[baseIndex+1] = quadSize;
            sizeArray[baseIndex+2] = quadSize;
            sizeArray[baseIndex+3] = quadSize;
 
            indexArray[index++] = (short)(baseIndex);           
            indexArray[index++] = (short)(baseIndex + 1);           
            indexArray[index++] = (short)(baseIndex + 2);           
            indexArray[index++] = (short)(baseIndex + 2);           
            indexArray[index++] = (short)(baseIndex + 3);           
            indexArray[index++] = (short)(baseIndex);
            
            baseIndex += 4;                       
        }
        
        Mesh mesh = new Mesh();
        
        FloatBuffer pb = BufferUtils.createFloatBuffer(posArray);
        FloatBuffer nb = BufferUtils.createFloatBuffer(normArray);
        FloatBuffer tb = BufferUtils.createFloatBuffer(uvArray);
        FloatBuffer sizeb = BufferUtils.createFloatBuffer(sizeArray);
        ShortBuffer sb = BufferUtils.createShortBuffer(indexArray);
 
        mesh.setBuffer(Type.Position, 3, pb);
        mesh.setBuffer(Type.Normal, 3, nb);
        mesh.setBuffer(Type.TexCoord, 4, tb);
        mesh.setBuffer(Type.Size, 1, sizeb);
        mesh.setBuffer(Type.Index, 3, sb);

        mesh.updateBound();                                
        mesh.createCollisionData();
 
        // Expand the bounds by the size so that the leaves don't clip
        // early.
        BoundingBox bb = (BoundingBox)mesh.getBound();
        Vector3f extents = bb.getExtent(null);
        extents.addLocal(quadSize * 0.6f, quadSize * 0.6f, quadSize * 0.6f);        
        bb.setXExtent(extents.x);  
        bb.setYExtent(extents.y);  
        bb.setZExtent(extents.z);
        mesh.setBound(bb);  
        
        return mesh;               
    }

}


