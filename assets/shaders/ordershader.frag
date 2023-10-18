uniform sampler2D u_texture;
uniform sampler2D u_texture2;

uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform vec2 u_regionsize;

uniform vec2 u_campos;
uniform vec2 u_srcpos;
uniform vec2 u_viewport;

uniform float u_time;

varying vec4 v_color;
varying vec2 v_texCoords;

vec4 sampleTexture(vec2 pos, vec2 offset){
    vec2 t = mod(pos / 2.0, 0.5) + offset;
    return texture2D(u_texture2, t);
}

void main(){
    vec2 cam = (u_campos - u_srcpos) / u_viewport;
    cam.y = -cam.y;


    vec4 c = texture2D(u_texture, v_texCoords);
    vec2 uv = ((v_texCoords - u_uv) / (u_uv2 - u_uv) - vec2(0.5, 0.5)) * u_regionsize / 128.0;
    float tim = -u_time / 180.0;
    float tim2 = -u_time / 110.0;
    float tim3 = -u_time / 90.0;
    float tim4 = -u_time / 75.0;

    vec4 l1 = sampleTexture(uv + vec2(tim * 1.5, tim) - cam, vec2(0.0, 0.0));
    vec4 l2 = sampleTexture(uv + vec2(tim2, tim2) - cam / 1.75, vec2(0.5, 0.0));
    vec4 l3 = sampleTexture(uv * 0.75 + vec2(tim3, tim3 * 0.75) - cam / 2.5, vec2(0.5, 0.5));
    vec4 l4 = sampleTexture(uv + vec2(tim4, tim4 * 0.5) - cam / 3.25, vec2(0.0, 0.5));

    //vec3 cl1 = mix(mix(l1.rgb, l2.rgb, l2.a), l4.rgb, l4.a);
    vec3 cl1 = mix(mix(mix(l1.rgb, l2.rgb, l2.a), l3.rgb, l3.a), l4.rgb, l4.a);

    gl_FragColor = vec4(cl1.rgb, c.a);
}
