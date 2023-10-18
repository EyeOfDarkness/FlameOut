#define MAX_LIGHTS 16

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec3 u_lights[MAX_LIGHTS];
uniform int u_lights_count;
uniform vec2 u_invsize;

varying vec2 v_texCoords;

void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    bool white = false;
    bool white2 = false;
    bool black = false;

    for(int i = 0; i < MAX_LIGHTS; i++){
        vec2 pos = u_lights[i].xy;
        float intensity = u_lights[i].z;
        
        vec2 dir = normalize((v_texCoords * u_texsize) - pos) * u_invsize;

        vec2 off = dir * intensity;
        //vec2 voff = v_texCoords - off;

        if(texture2D(u_texture, v_texCoords - off).a <= 0.01){
            white = true;
        }
        if(texture2D(u_texture, v_texCoords + dir * 2.0).a <= 0.01){
            black = true;
        }
        if(texture2D(u_texture, v_texCoords - dir * 2.5).a <= 0.01){
            white2 = true;
        }

        if(i >= u_lights_count - 1){
            break;
        }
    }

    if(white && (!black || white2)){
        gl_FragColor = vec4(1.0, 1.0, 1.0, c.a);
    }else{
        gl_FragColor = vec4(0.0, 0.0, 0.0, c.a);
    }
}
