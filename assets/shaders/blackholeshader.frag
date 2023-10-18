#define MAX_HOLES 8

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec4 u_holes[MAX_HOLES];
uniform int u_holes_count;
uniform vec2 u_invsize;
uniform float u_camscl;

varying vec2 v_texCoords;

float qcos(float deg){
    deg = mod(deg, 360.0);
    
    if(abs(deg) > 180.0){
        deg = 360.0 - abs(deg);
    }
    
    if(deg < 90.0 && deg > -90.0){
        float a = deg / 90.0;
        return 1.0 - (a * a);
    }else{
        float a = 1.0 - (abs(deg / 90.0) - 1.0);
        return (a * a) - 1.0;
    }
    return 0.0;
}
vec2 trns(vec2 pos, float r){
    float degRad = 0.0174533;
    float cos = cos(r * degRad);
    float sin = sin(r * degRad);
    
    return vec2(pos.x * cos - pos.y * sin, pos.x * sin + pos.y * cos);
}

void main(){
    vec2 tp = v_texCoords;
    //float inscl = max(u_invsize.x, u_invsize.y);

    for(int i = 0; i < MAX_HOLES; i++){
        vec2 pos = u_holes[i].xy;
        float intensity = u_holes[i].z;
        //float intensity = 1.0;
        float swirl = u_holes[i].w;

        float range = (30.0 + intensity * 170.0) * u_camscl;

        //vec2 vp = normalize((v_texCoords * u_texsize) - pos) * u_invsize;
        vec2 vp = (v_texCoords * u_texsize) - pos;
        float len = vp.x * vp.x + vp.y * vp.y;

        if(len < range * range){
            float dist = sqrt(len);
            float f = (range - dist) / range;
            vec2 nvp = normalize(vp);

            vec2 off = nvp * (intensity * 90.0 * f * f);
            //vec2 rot = (trns(vp, intensity * swirl * f * f * f) - vp) / u_camscl;
            vec2 rot = (trns(vp + off * u_camscl, intensity * swirl * f * f * f) - vp) / u_camscl;
            //vec2 rot = normalize(trns(vp, 2) - vp) / u_camscl;

            //vec4 nc = texture2D(u_texture, v_texCoords - off);
            //gl_FragColor = texture2D(u_texture, v_texCoords - off * u_invsize);
            //return;
            //tp = tp + (off + rot) * u_invsize;
            tp = tp + (rot) * u_invsize;
        }

        if(i >= u_holes_count - 1){
            break;
        }
    }

    vec4 c = texture2D(u_texture, tp);
    //c.a = 1.0;

    gl_FragColor = c;
}
