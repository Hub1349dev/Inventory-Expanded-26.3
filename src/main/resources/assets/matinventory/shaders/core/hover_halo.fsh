#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D Sampler0;
layout(std140) uniform Effect { vec4 effect; };

layout(location = 0) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

void main(){
    vec2 px=effect.x/vec2(textureSize(Sampler0,0));
    float center=texture(Sampler0,texCoord0).a,dilation=center;
    for(int y=-4;y<=4;y++)for(int x=-4;x<=4;x++){
        float distance=length(vec2(x,y));if(distance>4.25)continue;
        float core=1.0-smoothstep(.8,1.7,distance),falloff=.23*exp(-distance*distance/4.0);
        dilation=max(dilation,texture(Sampler0,texCoord0+vec2(x,y)*px).a*min(1.0,core+falloff));
    }
    float alpha=clamp(dilation-center,0.0,1.0);if(alpha<.025)discard;
    fragColor=vec4(1.0,1.0,1.0,alpha);
}

