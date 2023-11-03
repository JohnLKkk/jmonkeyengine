#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/LPVCommon.glsllib"
precision highp float;
attribute vec3 inPosition;

uniform lowp int m_GridSize;
uniform lowp int m_RsmSize;
uniform sampler2D m_RsmFlux;
uniform sampler2D m_RsmWorldPositions;
uniform sampler2D m_RsmWorldNormals;

struct RSMTexel
{
	vec3 world_position;
	vec3 world_normal;
	vec4 flux;
};

out RSMTexel vRsmTexel;

RSMTexel get_rsm_texel(ivec2 texCoord)
{
	RSMTexel texel;
	texel.world_normal = texelFetch(m_RsmWorldNormals, texCoord, 0).xyz;

	// Displace the position by half a normal
	texel.world_position = texelFetch(m_RsmWorldPositions, texCoord, 0).xyz + 0.5 * CELLSIZE * texel.world_normal;
	texel.flux = texelFetch(m_RsmFlux, texCoord, 0);
	return texel;
}

// Get ndc texture coordinates from gridcell
vec2 get_grid_output_position(ivec3 gridCell)
{
	float f_texture_size = float(m_GridSize);
	// Displace int coordinates with 0.5
	vec2 tex_coords = vec2((gridCell.x % m_GridSize) + m_GridSize * gridCell.z, gridCell.y) + vec2(0.5);
	// Get ndc coordinates
	vec2 ndc = vec2((2.0 * tex_coords.x) / (f_texture_size * f_texture_size), (2.0 * tex_coords.y) / f_texture_size) - vec2(1.0);
	return ndc;
}

void main()
{
	ivec2 rsm_tex_coords = ivec2(gl_VertexID % m_RsmSize, gl_VertexID / m_RsmSize);
	vRsmTexel = get_rsm_texel(rsm_tex_coords);
	ivec3 grid_cell = getGridCelli(vRsmTexel.world_position, m_GridSize);

	vec2 tex_coord = get_grid_output_position(grid_cell);

	gl_PointSize = 1.0f;
	gl_Position = vec4(tex_coord, 0.0, 1.0);
}
