#import "Common/ShaderLib/LPVCommon.glsllib"
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"

precision highp float;

#define FPI 3.1415926f

#if __VERSION__ >= 120
layout(location = 0) out vec4 outColor0;
layout(location = 1) out vec4 outColor1;
layout(location = 2) out vec4 outColor2;
#define o_red_color outColor0
#define o_green_color outColor1
#define o_blue_color outColor2
#else
#define o_red_color gl_FragData[0]
#define o_green_color gl_FragData[1]
#define o_blue_color gl_FragData[2]
#endif

struct RSMTexel {
	vec3 world_position;
	vec3 world_normal;
	vec4 flux;
};

uniform lowp int m_GridSize;
uniform lowp int m_RsmSize;

in RSMTexel vRsmTexel;

void main()
{
	float surfelWeight = float(m_GridSize) / float(m_RsmSize);
	vec4 SH_coeffs = (evalCosineLobeToDir(vRsmTexel.world_normal) / FPI) * surfelWeight;
	vec4 shR = SH_coeffs * vRsmTexel.flux.r;
	vec4 shG = SH_coeffs * vRsmTexel.flux.g;
	vec4 shB = SH_coeffs * vRsmTexel.flux.b;

	o_red_color = shR;
	o_green_color = shG;
	o_blue_color = shB;
}
