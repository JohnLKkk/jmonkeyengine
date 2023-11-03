#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/LPVCommon.glsllib"
precision highp float;

#define FPI 3.1415926f
#define DEG_TO_RAD FPI / 180.0f

uniform int m_TextureSize;
uniform int m_RsmSize;
uniform vec3 m_LightDirection;

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
out float vSurfelArea;

RSMTexel getRSMTexel(ivec2 texCoord)
{
	RSMTexel texel;
	texel.world_normal = texelFetch(m_RsmWorldNormals, texCoord, 0).xyz;

	// Displace the position by half a normal
	texel.world_position = texelFetch(m_RsmWorldPositions, texCoord, 0).xyz + 0.5 * texel.world_normal;
	texel.flux = texelFetch(m_RsmFlux, texCoord, 0);
	return texel;
}

// Get ndc texture coordinates from gridcell
vec2 getRenderingTexCoords(ivec3 gridCell)
{
	float f_texture_size = float(m_TextureSize);
	// Displace int coordinates with 0.5
	vec2 tex_coords = vec2((gridCell.x % m_TextureSize) + m_TextureSize * gridCell.z, gridCell.y) + vec2(0.5);
	// Get ndc coordinates
	vec2 ndc = vec2((2.0 * tex_coords.x) / (f_texture_size * f_texture_size), (2.0 * tex_coords.y) / f_texture_size) - vec2(1.0);
	return ndc;
}

// Sample from light
float calculateSurfelAreaLight(vec3 lightPos)
{
    float fov = 90.0f; //TODO fix correct fov
    float aspect = float(m_RsmSize / m_RsmSize);
    float tan_fov_x_half = tan(0.5 * fov * DEG_TO_RAD);
    float tan_fov_y_half = tan(0.5 * fov * DEG_TO_RAD) * aspect;

	return (4.0 * lightPos.z * lightPos.z * tan_fov_x_half * tan_fov_y_half) / float(m_RsmSize * m_RsmSize);
}

void main()
{
	ivec2 rsm_tex_coords = ivec2(gl_VertexID % m_RsmSize, gl_VertexID / m_RsmSize);
	vRsmTexel = getRSMTexel(rsm_tex_coords);
	ivec3 v_grid_cell = getGridCelli(vRsmTexel.world_position, m_TextureSize);

	vec2 tex_coord = getRenderingTexCoords(v_grid_cell);

	gl_PointSize = 1.0;
	gl_Position = vec4(tex_coord, 0.0, 1.0);

    vSurfelArea = calculateSurfelAreaLight(m_LightDirection);
}
