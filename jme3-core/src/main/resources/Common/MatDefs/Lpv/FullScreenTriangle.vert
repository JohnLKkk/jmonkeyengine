#import "Common/ShaderLib/GLSLCompat.glsllib"

attribute vec3 inPosition;

varying vec2 texCoord;

void main()
{
	texCoord = inPosition.xy * vec2(0.5f) + vec2(0.5f);
	gl_Position = vec4(inPosition, 1.0f);
}