uniform sampler2D u_texture;

varying vec4 v_color;
varying vec4 v_mix_color;
varying vec2 v_texCoords;

uniform vec4 u_main_color;

void main(){
    vec4 c = texture2D(u_texture, v_texCoords) * v_color;
    float l = (c.r + c.g + c.b) / 3.0;
    float l3 = max(0.0, (l - 0.8) / 0.2);
    float s = min((max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b))) + l3, 1.0);
    float a = c.a * u_main_color.a * v_mix_color.r;
    //float a = c.a * u_main_color.a;

    gl_FragColor = vec4(mix(vec3(l, l, l) * u_main_color.rgb, vec3(1.0, 1.0, 1.0), s * a), a);
}
