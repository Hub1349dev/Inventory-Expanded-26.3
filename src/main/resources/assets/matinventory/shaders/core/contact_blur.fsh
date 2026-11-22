#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;
layout(std140) uniform Effect { vec4 effect; };

layout(location = 0) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main(){
    vec2 stepUv=effect.xy/vec2(textureSize(Sampler0,0));
    float mask=texture(Sampler0,texCoord0).a*.227027027;
    mask+=texture(Sampler0,texCoord0+stepUv*1.384615385).a*.316216216;
    mask+=texture(Sampler0,texCoord0-stepUv*1.384615385).a*.316216216;
    mask+=texture(Sampler0,texCoord0+stepUv*3.230769231).a*.070270270;
    mask+=texture(Sampler0,texCoord0-stepUv*3.230769231).a*.070270270;
    fragColor=vec4(0.0,0.0,0.0,min(mask*effect.z,.55));
}

