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
    //((u_campos - u_srcpos) / u_camsize)
    //vec2 cam = (u_campos - u_srcpos) / (u_regionsize * 2.0);
    vec2 cam = (u_campos - u_srcpos) / u_viewport;
    //vec2 cam = vec2(0.0, 0.0);
    cam.y = -cam.y;
    
    //vec2 cam2 = cam / 2.0;
    //vec2 camoff = (u_srcpos - u_campos) / u_camsize;
    
    vec4 c = texture2D(u_texture, v_texCoords);
    vec2 uv = ((v_texCoords - u_uv) / (u_uv2 - u_uv) - vec2(0.5, 0.5)) * u_regionsize / 128.0;
    //vec2 uv = (((v_texCoords - u_uv) / (u_uv2 - u_uv) - camoff) * u_regionsize / 128.0) * u_camscl;
    float tim = u_time / 180.0;
    float tim2 = u_time / 110.0;
    float tim3 = u_time / 90.0;
    float tim4 = u_time / 65.0;
    
    float off = sin(((uv.y - cam.y + tim) * 6.0) - u_time / 40.0) / 8.0;
    float off2 = sin(((uv.y - (cam.y / 2.0) + tim2) * 4.0) - u_time / 30.0) / 8.0;

    vec4 l1 = sampleTexture(uv + vec2(off + tim, tim) - cam, vec2(0.0, 0.0));
    vec4 l2 = sampleTexture(uv + vec2(off2 + tim2, tim2) - cam / 2.0, vec2(0.5, 0.0));
    vec4 l3 = sampleTexture(uv + vec2(tim3 * 0.75, tim3) - cam / 3.0, vec2(0.0, 0.5));
    vec4 l4 = sampleTexture(uv + vec2(tim4, tim4 * 0.75) - cam / 4.0, vec2(0.5, 0.5));
    
    vec3 cl1 = mix(mix(mix(l1.rgb, l2.rgb, l2.a), l3.rgb, l3.a), l4.rgb, l4.a);
    //vec4 col = vec4(cl1.r, cl1.g, cl1.b, 1.0);

    //gl_FragColor = vec4(col.rgb, c.a);
    gl_FragColor = vec4(cl1.rgb, c.a);
}
