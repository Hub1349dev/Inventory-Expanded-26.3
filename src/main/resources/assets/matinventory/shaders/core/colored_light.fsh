#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 scene = texture(Sampler0, texCoord0);
    float peak = max(max(vertexColor.r, vertexColor.g), vertexColor.b);
    vec3 hue = vertexColor.rgb / max(peak, .001);
    hue *= hue;
    float luminance = dot(scene.rgb, vec3(.2126, .7152, .0722));
    vec3 illuminated = scene.rgb * .15 + luminance * hue * 1.35;
    vec3 lit = mix(scene.rgb, illuminated, .62 * vertexColor.a);
    #ifdef NATURAL_TINT
    {
        vec3 gentleHue=mix(vec3(1.0),vertexColor.rgb/max(peak,.001),.45);
        vec3 gentleLight=scene.rgb*(vec3(.90)+gentleHue*.25)+luminance*gentleHue*.12;
        lit=mix(scene.rgb,gentleLight,.30*vertexColor.a);
    }
    #endif
    float highlight = smoothstep(.68, .95, luminance);
    lit = mix(lit, min(scene.rgb * 1.35, vec3(1.0)), .90 * highlight * vertexColor.a);
    #ifdef PRESERVE_ALPHA
    fragColor = vec4(lit, scene.a) * ColorModulator;
    #else
    fragColor = vec4(lit, 1.0) * ColorModulator;
    #endif
}

