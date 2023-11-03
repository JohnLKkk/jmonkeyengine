#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/LPVCommon.glsllib"
precision highp float;
attribute vec3 inPosition;

uniform highp int m_GridSize;
flat out ivec2 vCellIndex;

vec2 get_grid_output_position()
{
    vec2 offset_position = vec2(inPosition.x, inPosition.y) + vec2(0.5); //offset position to middle of texel
    float f_grid_size = float(m_GridSize);

    return vec2((2.0 * offset_position.x) / (f_grid_size * f_grid_size), (2.0 * offset_position.y) / f_grid_size) - vec2(1.0);
}

void main()
{
    vec2 screen_pos = get_grid_output_position();

    vCellIndex = ivec2(int(inPosition.x), int(inPosition.y));

    gl_PointSize = 1.0;
    gl_Position = vec4(screen_pos, 0.0, 1.0);
}
