#import "Common/ShaderLib/GLSLCompat.glsllib"
//#import "Common/ShaderLib/Optics.glsllib"
// octahedral
#import "Common/ShaderLib/Octahedral.glsllib"

uniform sampler2D m_SphereSamples;
uniform int m_NumSamples;
uniform float m_LobeSize;

// For sampling "random" points in a sphere
#define NUM_SPHERE_SAMPLES 4096
#define DEFAULT_POW 2.2

uniform samplerCube m_Radiance;

varying vec2 texCoord;

///////////////////////////////////////////////////////////////////////////////

vec3 pointOnUnitSphere(int i, int n)
{
    // Spread out the n samples over the total amount (assuming n < sample count)
    int k = NUM_SPHERE_SAMPLES / n;

    int index = i * k;
    float u = (index % NUM_SPHERE_SAMPLES) * (1.0f / float(NUM_SPHERE_SAMPLES));
    vec3 point = texture2D(m_SphereSamples, vec2(u, 0.0f)).xyz;

    return point;
}

///////////////////////////////////////////////////////////////////////////////

void main() {
    vec3 N = octDecode(texCoord * 2.0f - 1.0f);
//    gl_FragColor = vec4(textureCube(m_Radiance, N).xyz, 1.0);


    vec3 irradiance = vec3(0.0);

    for (int i = 0; i < m_NumSamples; ++i)
    {
        // Importance sample points in the hemisphere using the cosine lobe method which
        // conveniently bakes in the LdotN and pdf terms.
        vec3 offset = pointOnUnitSphere(i, m_NumSamples);
        vec3 sampleDirection = normalize(N + m_LobeSize * offset);
        //vec3 sampleIrradiance = pow(textureCube(m_Radiance, sampleDirection).rgb, vec3(DEFAULT_POW));
        vec3 sampleIrradiance = textureCube(m_Radiance, sampleDirection).rgb;

        irradiance += sampleIrradiance;
    }

    // Average the samples together
    irradiance /= float(m_NumSamples);

    // Apply the Lambert BRDF (albedo / PI)
    // NOTE: Not relevant for now since we currently aren't physically based
    //irradiance /= PI;

    gl_FragColor = vec4(irradiance, 1.0);
    //gl_FragColor = vec4(textureCube(m_Radiance, N).rgb, 1.0);
}
