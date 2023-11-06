#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

#ifdef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"    
#endif

uniform vec4 m_Diffuse;

uniform vec4 g_LightColor;
uniform vec4 g_LightPosition;
uniform vec4 g_AmbientLightColor;

varying vec2 texCoord;
varying vec4 DiffuseSum;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;

varying vec3 lightVec;

#ifdef VERTEX_COLOR
  attribute vec4 inColor;
#endif

attribute vec4 inTangent;

varying vec3 vNormal;
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec4 vWorldPosition;
varying vec3 vWorldNormal;

void main(){
   vec4 modelSpacePos = vec4(inPosition, 1.0);
   vec3 modelSpaceNorm = inNormal;
   #ifndef VERTEX_LIGHTING
    vec3 modelSpaceTan  = inTangent.xyz;
   #endif
   
   #ifdef NUM_MORPH_TARGETS
        Morph_Compute(modelSpacePos, modelSpaceNorm);
   #endif

   #ifdef NUM_BONES
        #ifndef VERTEX_LIGHTING
        Skinning_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
        #endif
   #endif

    vWorldPosition = TransformWorld(modelSpacePos);
    vWorldNormal = TransformWorldNormal(modelSpaceNorm);
   gl_Position = TransformWorldViewProjection(modelSpacePos);// g_WorldViewProjectionMatrix * modelSpacePos;
   texCoord = inTexCoord;
   #ifdef SEPARATE_TEXCOORD
      texCoord2 = inTexCoord2;
   #endif

   vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;// (g_WorldViewMatrix * modelSpacePos).xyz;
   vec3 wvNormal  = normalize(TransformNormal(modelSpaceNorm));//normalize(g_NormalMatrix * modelSpaceNorm);
   vec3 viewDir = normalize(-wvPosition);
  
   vec4 wvLightPos = (g_ViewMatrix * vec4(g_LightPosition.xyz,clamp(g_LightColor.w,0.0,1.0)));
   wvLightPos.w = g_LightPosition.w;
   vec4 lightColor = g_LightColor;

   #if (defined(NORMALMAP) || defined(PARALLAXMAP)) && !defined(VERTEX_LIGHTING)
     vec3 wvTangent = normalize(TransformNormal(modelSpaceTan));
     vec3 wvBinormal = cross(wvNormal, wvTangent);
     mat3 tbnMat = mat3(wvTangent, wvBinormal * inTangent.w,wvNormal);
   #endif

   vNormal = wvNormal;
   #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
        vViewDir  = -wvPosition * tbnMat;
   #if (defined(PARALLAXMAP) || (defined(NORMALMAP_PARALLAX) && defined(NORMALMAP)))
        vViewDirPrlx = vViewDir;
   #endif
        lightComputeDir(wvPosition, lightColor.w, wvLightPos, vLightDir, lightVec);
        vLightDir.xyz = (vLightDir.xyz * tbnMat).xyz;
   #elif !defined(VERTEX_LIGHTING)
    vViewDir = viewDir;
    lightComputeDir(wvPosition, lightColor.w, wvLightPos, vLightDir, lightVec);
   #endif

   #ifdef MATERIAL_COLORS
      DiffuseSum  =  m_Diffuse  * vec4(lightColor.rgb, 1.0);
    #else
      DiffuseSum  =  vec4(lightColor.rgb, 1.0);
    #endif
}