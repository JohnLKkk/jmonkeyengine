#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Deferred.glsllib"
// shading model
#import "Common/ShaderLib/ShadingModel.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"

varying vec3 wNormal;
varying vec3 wPosition;
void main(){
    Context_OutGBuff3.xyz = wNormal;
    Context_OutGBuff2.a = LIGHT_PROBE_DEBUG;
}
