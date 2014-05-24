#import "Common/ShaderLib/Skinning.glsllib"
attribute vec3 inPosition;
attribute vec3 inNormal;
attribute vec4 inTexCoord;
attribute float inSize;

uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldViewMatrix;
uniform mat4 g_ProjectionMatrix;
uniform mat3 g_NormalMatrix;

varying vec2 texCoord;

void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;

   #ifdef NUM_BONES
       Skinning_Compute(modelSpacePos);
   #endif
 
    // ** Calculate in viewspace... the billboarding will crawl
    //    as the camera turns but it's fine for shadows and
    //    requires fewer transforms    
    vec3 wvPosition = (g_WorldViewMatrix * modelSpacePos).xyz;
    
    // ** The normal in this case is really the axis 
    vec3 wvNormal = g_NormalMatrix * modelSpaceNorm;
 
    // ** Simple x,y inversion works for an orthogonal vector       
    vec3 offset = normalize(vec3(wvNormal.y, -wvNormal.x, 0.0));
    wvPosition += offset * inSize;
 
    // ** Now to projection space   
    gl_Position = g_ProjectionMatrix * vec4(wvPosition, 1.0);
  
    // ** old calculation replaced by the above
    // gl_Position = g_WorldViewProjectionMatrix * modelSpacePos;
    
    texCoord = inTexCoord;
}
