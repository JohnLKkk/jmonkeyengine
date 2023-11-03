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

uniform vec3 m_LightDirection;

struct RSMTexel {
	vec3 world_position;
	vec3 world_normal;
	vec4 flux;
};

in RSMTexel vRsmTexel;
in float vSurfelArea;

float calculateBlockingPotencial(vec3 dir, vec3 normal)
{
	return clamp((vSurfelArea * clamp(dot(normal, dir), 0.0, 1.0)) / (CELLSIZE * CELLSIZE), 0.0, 1.0); //It is probability so 0.0 - 1.0
}

//#define DEBUG_RENDER

void main()
{
    //Discard pixels with really small normal
	if (length(vRsmTexel.world_normal) < 0.01) {
		discard;
	}

	vec3 light_dir = normalize(m_LightDirection - vRsmTexel.world_position); //Both are in world space
	float blocking_potencial = calculateBlockingPotencial(light_dir, vRsmTexel.world_normal);

	vec4 SH_coeffs = evalCosineLobeToDir(vRsmTexel.world_normal) * blocking_potencial;
	vec4 shR = SH_coeffs * vRsmTexel.flux.r;
    vec4 shG = SH_coeffs * vRsmTexel.flux.g;
    vec4 shB = SH_coeffs * vRsmTexel.flux.b;

	#ifdef DEBUG_RENDER
    	o_red_color = vec4(normalize(shR.xyz),1.0);
    	o_green_color = vec4(normalize(shG.xyz),1.0);
    	o_blue_color = vec4(normalize(shB.xyz),1.0);
    #else
    	o_red_color = shR;
    	o_green_color = shG;
    	o_blue_color = shB;
    #endif
}
