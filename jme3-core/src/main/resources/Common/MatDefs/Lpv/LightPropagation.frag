#import "Common/ShaderLib/LPVCommon.glsllib"
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Optics.glsllib"
precision highp float;

#define FPI 3.1415926f


uniform highp int m_GridSize;

uniform sampler2D m_RedContribution;
uniform sampler2D m_GreenContribution;
uniform sampler2D m_BlueContribution;

uniform sampler2D m_RedGeometryVolume;
uniform sampler2D m_GreenGeometryVolume;
uniform sampler2D m_BlueGeometryVolume;

uniform bool m_FirstIteration;

flat in ivec2 vCellIndex;

#if __VERSION__ >= 120
layout(location = 0) out vec4 outColor0;
layout(location = 1) out vec4 outColor1;
layout(location = 2) out vec4 outColor2;

layout(location = 3) out vec4 outColor3;
layout(location = 4) out vec4 outColor4;
layout(location = 5) out vec4 outColor5;
#define o_red_color outColor0
#define o_green_color outColor1
#define o_blue_color outColor2
#define o_next_iteration_red_color outColor3
#define o_next_iteration_green_color outColor4
#define o_next_iteration_blue_color outColor5
#else
#define o_red_color gl_FragData[0]
#define o_green_color gl_FragData[1]
#define o_blue_color gl_FragData[2]
#define o_next_iteration_red_color gl_FragData[3]
#define o_next_iteration_green_color gl_FragData[4]
#define o_next_iteration_blue_color gl_FragData[5]
#endif

vec4 red_contribution = vec4(0.0);
vec4 green_contribution = vec4(0.0);
vec4 blue_contribution = vec4(0.0);
float occlusion_amplifier = 1.0f;

// orientation = [ right | up | forward ] = [ x | y | z ]
const mat3 neighbourOrientations[6] = mat3[] (
    // Z+
    mat3(1, 0, 0,0, 1, 0,0, 0, 1),
    // Z-
    mat3(-1, 0, 0,0, 1, 0,0, 0, -1),
    // X+
    mat3(0, 0, 1,0, 1, 0,-1, 0, 0
        ),
    // X-
    mat3(0, 0, -1,0, 1, 0,1, 0, 0),
    // Y+
    mat3(1, 0, 0,0, 0, 1,0, -1, 0),
    // Y-
    mat3(1, 0, 0,0, 0, -1,0, 1, 0)
);

// Faces in cube
const ivec2 sideFaces[4] = ivec2[] (
    ivec2(1, 0),   // right
    ivec2(0, 1),   // up
    ivec2(-1, 0),  // left
    ivec2(0, -1)   // down
);

vec3 get_eval_side_direction(int index, mat3 orientation)
{
    const float small_component = 0.4472135; // 1 / sqrt(5)
    const float big_component = 0.894427; // 2 / sqrt(5)

    vec2 current_side = vec2(sideFaces[index]);
    return orientation * vec3(current_side.x * small_component, current_side.y * small_component, big_component);
}

vec3 get_reproj_side_direction(int index, mat3 orientation)
{
    ivec2 current_side = sideFaces[index];
    return orientation * vec3(current_side.x, current_side.y, 0);
}

void propagate()
{
    // Use solid angles to avoid inaccurate integral value stemming from low-order SH approximations
    const float direct_face_solid_angle = 0.4006696846f / FPI;
	const float side_face_solid_angle = 0.4234413544f / FPI;

    // Add contributions of neighbours to this cell
    for (int neighbour = 0; neighbour < 6; neighbour++)
    {
        mat3 orientation = neighbourOrientations[neighbour];
        vec3 direction = orientation * vec3(0.0, 0.0, 1.0);

        // Index offset in our flattened version of the lpv grid
        ivec2 index_offset = ivec2(
            direction.x + (direction.z * float(m_GridSize)),
            direction.y
        );

        ivec2 neighbour_index = vCellIndex - index_offset;

        vec4 red_contribution_neighbour = texelFetch(m_RedContribution, neighbour_index, 0);
        vec4 green_contribution_neighbour = texelFetch(m_GreenContribution, neighbour_index, 0);
        vec4 blue_contribution_neighbour = texelFetch(m_BlueContribution, neighbour_index, 0);

        // No occlusion
        float red_occlusion_val = 1.0;
        float green_occlusion_val = 1.0;
        float blue_occlusion_val = 1.0;

        // No occlusion in the first step
        if (!m_FirstIteration) {
            vec3 h_direction = 0.5 * direction;
            ivec2 offset = ivec2(
                h_direction.x + (h_direction.z * float(m_GridSize)),
                h_direction.y
            );
            ivec2 occ_coord = vCellIndex - offset;

            vec4 red_occ_coeffs = texelFetch(m_RedGeometryVolume, occ_coord, 0);
            vec4 green_occ_coeffs = texelFetch(m_GreenGeometryVolume, occ_coord, 0);
            vec4 blue_occ_coeffs = texelFetch(m_BlueGeometryVolume, occ_coord, 0);

            red_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(red_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
            green_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(green_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
            blue_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(blue_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
        }

        float occluded_direct_face_red_contribution = red_occlusion_val * direct_face_solid_angle;
        float occluded_direct_face_green_contribution = green_occlusion_val * direct_face_solid_angle;
        float occluded_direct_face_blue_contribution = blue_occlusion_val * direct_face_solid_angle;

        vec4 direction_cosine_lobe = evalCosineLobeToDir(direction);
        vec4 direction_spherical_harmonic = dirToSH(direction);

        red_contribution += occluded_direct_face_red_contribution * max(0.0, dot(red_contribution_neighbour, direction_spherical_harmonic)) * direction_cosine_lobe;
        green_contribution += occluded_direct_face_green_contribution * max(0.0, dot( green_contribution_neighbour, direction_spherical_harmonic)) * direction_cosine_lobe;
        blue_contribution += occluded_direct_face_blue_contribution * max(0.0, dot(blue_contribution_neighbour, direction_spherical_harmonic)) * direction_cosine_lobe;

        // Add contributions of faces of neighbour
        for (int face = 0; face < 4; face++)
        {
            vec3 eval_direction = get_eval_side_direction(face, orientation);
            vec3 reproj_direction = get_reproj_side_direction(face, orientation);

            // No occlusion in the first step
            if (!m_FirstIteration) {
                vec3 h_direction = 0.5 * direction;
                ivec2 offset = ivec2(
                    h_direction.x + (h_direction.z * float(m_GridSize)),
                    h_direction.y
                );
                ivec2 occ_coord = vCellIndex - offset;

                vec4 red_occ_coeffs = texelFetch(m_RedGeometryVolume, occ_coord, 0);
                vec4 green_occ_coeffs = texelFetch(m_GreenGeometryVolume, occ_coord, 0);
                vec4 blue_occ_coeffs = texelFetch(m_BlueGeometryVolume, occ_coord, 0);

                red_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(red_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
                green_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(green_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
                blue_occlusion_val = 1.0 - clamp(occlusion_amplifier * dot(blue_occ_coeffs, dirToSH(-direction)), 0.0, 1.0);
            }

            float occluded_side_face_red_contribution = red_occlusion_val * side_face_solid_angle;
            float occluded_side_face_green_contribution = green_occlusion_val * side_face_solid_angle;
            float occluded_side_face_blue_contribution = blue_occlusion_val * side_face_solid_angle;

            vec4 reproj_direction_cosine_lobe = evalCosineLobeToDir(reproj_direction);
			vec4 eval_direction_spherical_harmonic = dirToSH(eval_direction);

		    red_contribution += occluded_side_face_red_contribution * max(0.0, dot(red_contribution_neighbour, eval_direction_spherical_harmonic)) * reproj_direction_cosine_lobe;
			green_contribution += occluded_side_face_green_contribution * max(0.0, dot(green_contribution_neighbour, eval_direction_spherical_harmonic)) * reproj_direction_cosine_lobe;
			blue_contribution += occluded_side_face_blue_contribution * max(0.0, dot(blue_contribution_neighbour, eval_direction_spherical_harmonic)) * reproj_direction_cosine_lobe;
        }
    }
}

void main()
{
    propagate();

    o_red_color = red_contribution;
    o_green_color = green_contribution;
    o_blue_color = blue_contribution;

    o_next_iteration_red_color = red_contribution;
    o_next_iteration_green_color = green_contribution;
    o_next_iteration_blue_color = blue_contribution;
}