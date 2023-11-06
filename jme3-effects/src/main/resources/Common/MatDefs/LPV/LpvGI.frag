#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/ShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"
// begin-lpvgi@jhonkkk
#ifdef APPLY_LPV_GI
#import "Common/ShaderLib/LPVCommon.glsllib"
#endif
// end-lpvgi@jhonkkk

#if defined(USE_LIGHTS_CULL_MODE)
uniform vec2 g_ResolutionInverse;
#else
varying vec2 texCoord;
#endif
varying mat4 viewProjectionMatrixInverse;
uniform COLORTEXTURE m_Texture;

void main() {
    vec2 innerTexCoord;
#if defined(USE_LIGHTS_CULL_MODE)
    innerTexCoord = gl_FragCoord.xy * g_ResolutionInverse;
#else
    innerTexCoord = texCoord;
#endif
    vec4 diffuseColor = texture2D(Context_InGBuff0, innerTexCoord);
    vec4 n1n2 = texture2D(Context_InGBuff3, innerTexCoord);
    vec3 wNorm = octDecode(n1n2.zw);
    vec3 wPos = getPosition(innerTexCoord, viewProjectionMatrixInverse);
    gl_FragColor = getColor(m_Texture, innerTexCoord);

    #ifdef APPLY_LPV_GI
        vec3 lpv_intensity = get_lpv_intensity(wNorm, wPos);
        vec3 lpv_radiance = vec3(max(0.0, lpv_intensity.r), max(0.0, lpv_intensity.g), max(0.0, lpv_intensity.b)) / (3.14159265358979323846);
        vec3 indirect_light = diffuseColor.rgb * lpv_radiance;
        gl_FragColor.rgb += indirect_light * m_IndirectLightAttenuation;
    #endif
}