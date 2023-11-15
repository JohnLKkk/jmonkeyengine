#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
attribute vec3 inNormal;
varying vec3 wNormal;
varying vec3 wPosition;
void main(){
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    wPosition = TransformWorld(modelSpacePos).xyz;
    wNormal  = normalize(TransformWorldNormal(inNormal));
    gl_Position = TransformWorldViewProjection(modelSpacePos);
}