#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"

#if __VERSION__ >= 120
layout(location = 0) out vec4 outColor0;
layout(location = 1) out vec4 outColor1;
layout(location = 2) out vec4 outColor2;
#define o_color_map outColor0
#define o_position_map outColor1
#define o_normal_map outColor2
#else
#define o_color_map gl_FragData[0]
#define o_position_map gl_FragData[1]
#define o_normal_map gl_FragData[2]
#endif


#ifndef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"
    #import "Common/ShaderLib/Lighting.glsllib"
#endif

float computeDiffuseLighting(in vec3 norm, in vec3 viewDir, in vec3 lightDir, in float attenuation){
   float diffuseFactor = lightComputeDiffuse(norm, lightDir);
   return diffuseFactor * attenuation;
}

uniform vec4 g_LightDirection;
varying vec2 texCoord;
varying vec4 DiffuseSum;
#ifdef DIFFUSEMAP
  uniform sampler2D m_DiffuseMap;
#endif
varying vec3 vViewDir;
varying vec4 vLightDir;
varying vec3 vNormal;
varying vec3 lightVec;
varying vec4 vWorldPosition;
varying vec3 vWorldNormal;

void main(){
    vec2 newTexCoord = texCoord;
     
   #ifdef DIFFUSEMAP
      vec4 diffuseColor = texture2D(m_DiffuseMap, newTexCoord);
    #else
      vec4 diffuseColor = vec4(1.0);
    #endif

    float alpha = DiffuseSum.a * diffuseColor.a;
    vec3 normal = vNormal;

    vec4 lightDir = vLightDir;
    lightDir.xyz = normalize(lightDir.xyz);
    vec3 viewDir = normalize(vViewDir);
    float spotFallOff = 1.0;

   #if __VERSION__ >= 110
    // allow use of control flow
    if(g_LightDirection.w != 0.0){
   #endif
      spotFallOff =  computeSpotFalloff(g_LightDirection, lightVec);
   #if __VERSION__ >= 110
     }
   #endif

   float diffuseLight = computeDiffuseLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff) ;
   o_color_map.rgb = DiffuseSum.rgb   * diffuseColor.rgb  * vec3(diffuseLight);
    o_color_map.a = alpha;
    o_position_map = vWorldPosition;
    o_normal_map = vec4(vWorldNormal, 1.0f);
}
