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
//#define USE_LIGHT_COMPUTE

#ifdef NORMALMAP
uniform sampler2D m_NormalMap;
#endif

#if defined(LEGACY_LIGHTING) || defined(STANDARD_LIGHTING)

    #ifndef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"
    #import "Common/ShaderLib/Lighting.glsllib"
    #endif
    float computeDiffuseLighting(in vec3 norm, in vec3 viewDir, in vec3 lightDir, in float attenuation){
        float diffuseFactor = lightComputeDiffuse(norm, lightDir);
        return diffuseFactor * attenuation;
    }

#endif

#ifdef STANDARD_LIGHTING
    uniform vec4 g_LightDirection;
    varying vec4 DiffuseSum;
    varying vec3 vViewDir;
    varying vec4 vLightDir;
    varying vec3 lightVec;
    varying vec3 vNormal;
    uniform float m_Metallic;
    #ifdef BASECOLORMAP
    uniform sampler2D m_BaseColorMap;
    #endif
    #ifdef USE_PACKED_MR
    uniform sampler2D m_MetallicRoughnessMap;
    #else
    #ifdef METALLICMAP
    uniform sampler2D m_MetallicMap;
    #endif
    #endif
#endif





varying vec2 texCoord;

#ifdef LEGACY_LIGHTING
    uniform vec4 g_LightDirection;
    varying vec4 DiffuseSum;
    varying vec3 vViewDir;
    varying vec4 vLightDir;
    varying vec3 lightVec;
    varying vec3 vNormal;
    #ifdef DIFFUSEMAP
      uniform sampler2D m_DiffuseMap;
    #endif
#endif
varying vec4 vWorldPosition;
varying vec3 vWorldNormal;

void main(){
    vec2 newTexCoord = texCoord;
    float alpha = 1.0f;
    vec3 result;

    #ifdef LEGACY_LIGHTING
        #ifdef DIFFUSEMAP
          vec4 diffuseColor = texture2D(m_DiffuseMap, newTexCoord);
          diffuseColor.rgb = pow(diffuseColor.rgb, vec3(2.2f));
        #else
          vec4 diffuseColor = vec4(1.0);
        #endif

        alpha = DiffuseSum.a * diffuseColor.a;
        #ifdef USE_LIGHT_COMPUTE
            #if defined(NORMALMAP)
                vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
                //Note the -2.0 and -1.0. We invert the green channel of the normal map,
                //as it's compliant with normal maps generated with blender.
                //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
                //for more explanation.
                vec3 normal = normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
            #else
                vec3 normal = vNormal;
            #endif

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
           result = DiffuseSum.rgb   * diffuseColor.rgb  * vec3(diffuseLight);
        #else
           result = DiffuseSum.rgb   * diffuseColor.rgb;
        #endif
    #endif


    #ifdef STANDARD_LIGHTING
       #ifdef BASECOLORMAP
        vec4 albedo = texture2D(m_BaseColorMap, newTexCoord);
       #else
        vec4 albedo = vec4(1);
       #endif
       #ifdef USE_PACKED_MR
        vec3 aoRoughnessMetallicValue = texture2D(m_MetallicRoughnessMap, newTexCoord).rgb;
        float Metallic = aoRoughnessMetallicValue.b * max(m_Metallic, 0.0);
        #else
            #ifdef METALLICMAP
                float Metallic = texture2D(m_MetallicMap, newTexCoord).r * max(m_Metallic, 0.0);
            #else
                float Metallic =  max(m_Metallic, 0.0);
            #endif
        #endif
        alpha = albedo.a;
        vec4 diffuseColor = albedo - albedo * Metallic;
        diffuseColor.rgb = pow(diffuseColor.rgb, vec3(2.2f));

        #ifdef USE_LIGHT_COMPUTE
            #if defined(NORMALMAP)
                vec4 normalHeight = texture2D(m_NormalMap, newTexCoord);
                //Note the -2.0 and -1.0. We invert the green channel of the normal map,
                //as it's compliant with normal maps generated with blender.
                //see http://hub.jmonkeyengine.org/forum/topic/parallax-mapping-fundamental-bug/#post-256898
                //for more explanation.
                vec3 normal = normalize((normalHeight.xyz * vec3(2.0,-2.0,2.0) - vec3(1.0,-1.0,1.0)));
            #else
                vec3 normal = vNormal;
            #endif

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
            result = DiffuseSum.rgb   * diffuseColor.rgb  * vec3(diffuseLight);
        #else
            result = DiffuseSum.rgb   * diffuseColor.rgb;
        #endif
    #endif
    o_color_map.rgb = result;
    o_color_map.a = alpha;
    o_position_map = vWorldPosition;
    o_normal_map = vec4(vWorldNormal, 1.0f);
}
