#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
#import "Common/ShaderLib/Parallax.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
#import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/PBR.glsllib"
#import "Common/ShaderLib/ShadingModel.glsllib"
#import "jme3test/materials/CustomShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"
// skyLight and reflectionProbe
uniform vec4 g_AmbientLightColor;
#import "Common/ShaderLib/SkyLightReflectionProbe.glsllib"
#if defined(USE_LIGHTS_CULL_MODE)
    uniform vec2 g_ResolutionInverse;
#else
    varying vec2 texCoord;
#endif

varying mat4 viewProjectionMatrixInverse;
uniform mat4 g_ViewMatrix;
uniform vec3 g_CameraPosition;
uniform int m_NBLight;



#if defined(USE_TEXTURE_PACK_MODE)
    uniform int g_LightCount;
    uniform sampler2D m_LightPackData1;
    uniform sampler2D m_LightPackData2;
    uniform sampler2D m_LightPackData3;
#else
    uniform vec4 g_LightData[NB_LIGHTS];
#endif

uniform vec2 g_Resolution;
uniform int g_TileSize;
uniform int g_WidthTile;
uniform int g_HeightTile;
uniform int g_TileLightOffsetSize;
uniform sampler2D m_TileLightDecode;
uniform sampler2D m_TileLightIndex;

void main(){
    vec2 innerTexCoord;
#if defined(USE_LIGHTS_CULL_MODE)
    innerTexCoord = gl_FragCoord.xy * g_ResolutionInverse;
#else
    innerTexCoord = texCoord;
#endif
    // unpack GBuffer
    vec4 buff0 = texture2D(Context_InGBuff0, innerTexCoord);
    vec4 buff1 = texture2D(Context_InGBuff1, innerTexCoord);
    int shadingModelId = int(floor(buff0.a));
    if(shadingModelId == MY_CUSTOM_PHONG_LIGHTING){
        vec3 vPos = getPosition(innerTexCoord, viewProjectionMatrixInverse);
        vec4 diffuseColor = buff0;
        vec3 specularColor = floor(buff1.rgb) * 0.01f;
        vec3 AmbientSum = min(fract(buff1.rgb) * 100.0f, vec3(1.0f)) * g_AmbientLightColor.rgb;
        float Shininess = buff1.a;
        float alpha = min(fract(diffuseColor.a) * 100.0f, 0.0f);
        vec3 normal = -approximateNormal(vPos, innerTexCoord, viewProjectionMatrixInverse).xyz;
        vec3 viewDir  = normalize(g_CameraPosition - vPos);

        gl_FragColor.rgb = AmbientSum * diffuseColor.rgb;
        gl_FragColor.a = alpha;
        int lightNum = 0;
        #if defined(USE_TEXTURE_PACK_MODE)
        float lightTexSizeInv = 1.0f / (float(PACK_NB_LIGHTS) - 1.0f);
        lightNum = m_NBLight;
        #else
        lightNum = NB_LIGHTS;
        float lightTexSizeInv = 1.0f / (float(lightNum) - 1.0f);
        #endif

        // Tile Based Shading
        // get the grid data index
        vec2 gridIndex = vec2(((innerTexCoord.x*g_Resolution.x) / float(g_TileSize)) / float(g_WidthTile), ((innerTexCoord.y*g_Resolution.y) / float(g_TileSize)) / float(g_HeightTile));
        // get tile info
        vec3 tile = texture2D(m_TileLightDecode, gridIndex).xyz;
        int uoffset = int(tile.x);
        int voffset = int(tile.z);
        int count = int(tile.y);
        if(count > 0){
            int lightId;
            float temp;
            int offset;
            // Normalize lightIndex sampling range to unit space
            float uvSize = 1.0f / (g_TileLightOffsetSize - 1.0f);
            vec2 lightUV;
            vec2 lightDataUV;
            for(int i = 0;i < count;){
                temp = float(uoffset + i);
                offset = 0;

                if(temp >= g_TileLightOffsetSize){
                    //temp -= g_TileLightOffsetSize;
                    offset += int(temp / float(g_TileLightOffsetSize));
                    temp = float(int(temp) % g_TileLightOffsetSize);
                }
                if(temp == g_TileLightOffsetSize){
                    temp = 0.0f;
                }

                // lightIndexUV
                lightUV = vec2(temp * uvSize, float(voffset + offset) * uvSize);
                lightId = int(texture2D(m_TileLightIndex, lightUV).x);
                lightDataUV = vec2(float(lightId) * lightTexSizeInv, 0.0f);

                #if defined(USE_TEXTURE_PACK_MODE)
                    vec4 lightColor = texture2D(m_LightPackData1, lightDataUV);
                    vec4 lightData1 = texture2D(m_LightPackData2, lightDataUV);
                #else
                    vec4 lightColor = g_LightData[lightId*3];
                    vec4 lightData1 = g_LightData[lightId*3+1];
                #endif
                    vec4 lightDir;
                    vec3 lightVec;
                    lightComputeDir(vPos, lightColor.w, lightData1, lightDir,lightVec);

                    float spotFallOff = 1.0;
                #if __VERSION__ >= 110
                    // allow use of control flow
                    if(lightColor.w > 1.0){
                #endif
                #if defined(USE_TEXTURE_PACK_MODE)
                    spotFallOff =  computeSpotFalloff(texture2D(m_LightPackData3, lightDataUV), lightVec);
                #else
                    spotFallOff =  computeSpotFalloff(g_LightData[lightId*3+2], lightVec);
                #endif
                #if __VERSION__ >= 110
                }
                #endif

                #ifdef NORMALMAP
                    //Normal map -> lighting is computed in tangent space
                    lightDir.xyz = normalize(lightDir.xyz * tbnMat);
                #else
                    //no Normal map -> lighting is computed in view space
                    lightDir.xyz = normalize(lightDir.xyz);
                #endif

                vec2 light = computeLighting(normal, viewDir, lightDir.xyz, lightDir.w * spotFallOff , Shininess);

                gl_FragColor.rgb += lightColor.rgb * diffuseColor.rgb  * vec3(light.x) +
                lightColor.rgb * specularColor.rgb * vec3(light.y);
                #if defined(USE_TEXTURE_PACK_MODE)
                i++;
                #else
                i++;
                #endif
            }
        }
    }
    else{
        // todo:Calling the shading model library function encapsulated inside the system
        gl_FragColor = vec4(0.0f, 0.0f, 0.0f, 1);
    }
}
