#import "Common/ShaderLib/GLSLCompat.glsllib"

varying vec3 v_Position;

void main(){
    float distance_to_fragment = length(v_Position);
    gl_FragColor = vec4(distance_to_fragment, distance_to_fragment * distance_to_fragment, 0.0, 0.0);
}
