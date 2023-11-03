#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
varying vec2 texCoord;

uniform sampler2D m_Texture;

void main()
{
	gl_FragColor = texture(m_Texture, texCoord);
}