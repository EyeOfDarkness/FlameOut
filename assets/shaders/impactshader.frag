#define MAX_LIGHTS 8

varying lowp vec4 v_color;
varying lowp vec4 v_mix_color;
varying vec2 v_texCoords;
varying vec4 v_position;

uniform sampler2D u_texture;
uniform vec3 u_lights[MAX_LIGHTS];
uniform int u_lights_count;

uniform vec4 u_uv;
uniform vec2 u_texscl;
uniform vec2 u_pos;

void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    bool white = false;

    for(int i = 0; i < MAX_LIGHTS; i++){
        vec2 pos = u_lights[i].xy;
        float intensity = u_lights[i].z;

        vec2 off = normalize(v_position.xy - pos) * intensity * u_texscl;
        //vec2 off = normalize(gl_FragCoord.xy - pos) * intensity * u_texscl;
        //vec2 tf = vec2(off.x * u_sincos.y - off.y * u_sincos.x, off.x * u_sincos.x + off.y * u_sincos.y);
        vec2 voff = v_texCoords + off;

        if((voff.x < u_uv.x || voff.y < u_uv.y || voff.x > u_uv.z || voff.y > u_uv.w) || texture2D(u_texture, voff).a <= 0.01){
            white = true;
            break;
        }

        if(i >= u_lights_count - 1){
            break;
        }
    }

    if(white){
        gl_FragColor = vec4(1.0, 0.0, 0.0, c.a);
    }else{
        gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);
    }
}
