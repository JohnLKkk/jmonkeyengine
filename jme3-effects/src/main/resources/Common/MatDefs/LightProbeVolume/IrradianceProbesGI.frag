#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"
#import "Common/ShaderLib/ShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"
// begin-irradiance_probes_gi@jhonkkk
#ifdef APPLY_IRRADIANCE_PROBES_GI
#import "Common/ShaderLib/LightFieldProbe2.glsllib"
#endif
// end-irradiance_probes_gi@jhonkkk

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
    vec4 shadingInfo = texture2D(Context_InGBuff2, innerTexCoord);
    int shadingModelId = int(floor(shadingInfo.a));
    vec4 diffuseColor = texture2D(Context_InGBuff0, innerTexCoord);
    vec4 n1n2 = texture2D(Context_InGBuff3, innerTexCoord);
    vec3 wNorm = octDecode(n1n2.zw);
    vec3 wPos = getPosition(innerTexCoord, viewProjectionMatrixInverse);
    gl_FragColor = getColor(m_Texture, innerTexCoord);

    if(IS_LIT(shadingModelId)){
        #ifdef APPLY_IRRADIANCE_PROBES_GI
            bool calcGI = false;
            vec3 irradiance = applyLightProbeVolume(diffuseColor.rgb, wPos, normalize(wNorm));
            gl_FragColor.rgb += irradiance;
        #endif
    }
}