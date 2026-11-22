#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;

layout(location = 2) in vec4 vertexColor;
layout(location = 6) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < .001) discard;
    float weave = dot(texel.rgb, vec3(.2126, .7152, .0722)) / .50;
    fragColor = vec4(clamp(vec3(weave) * vertexColor.rgb, 0.0, 1.0), texel.a * vertexColor.a);
}
