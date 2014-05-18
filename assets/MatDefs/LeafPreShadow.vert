#import "Common/ShaderLib/Skinning.glsllib"
attribute vec3 inPosition;
attribute vec4 inTexCoord;
attribute float inSize;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ProjectionMatrix;

varying vec2 texCoord;

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);

   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
 
    // ** Calculate camera parallel billboarding which is
    //    the best for shadows.  
    vec2 corner = inTexCoord.xy;
 
    // ** Calculate the world view position   
    vec3 wvPosition = (g_WorldViewMatrix * modelSpacePos).xyz;
    wvPosition.x += (corner.x - 0.5) * inSize;
    wvPosition.y += (corner.y - 0.5) * inSize;
        
    // ** Push it away a little bit to see if it helps with the hard
    //    shadow line when the billboarded quads end up shadowing themselves
    wvPosition.z -= inSize * 0.5;
 
    // ** Now to view space   
    gl_Position = g_ProjectionMatrix * vec4(wvPosition, 1.0);
  
    // ** old calculation replaced by the above
    // gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
    
    // ** Texture coordinate now in zw instead of xy
    texCoord = inTexCoord.zw;
}
