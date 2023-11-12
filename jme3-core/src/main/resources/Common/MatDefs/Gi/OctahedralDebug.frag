#import "Common/ShaderLib/GLSLCompat.glsllib"

#ifdef TEXTURE
uniform sampler2DArray m_OctahedralData;
varying vec2 texCoord;
#endif
uniform int m_ProbeIndex;

varying vec4 color;

void main() {
    #ifdef TEXTURE
    vec4 texVal = texture2DArray(m_OctahedralData, vec3(texCoord, m_ProbeIndex));
    gl_FragColor = vec4(texVal.xyz, 1.0f);
    #else
    gl_FragColor = color;
    #endif
}