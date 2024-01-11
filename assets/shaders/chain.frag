uniform sampler2D u_texture;

uniform vec2 u_uv;
uniform vec2 u_uv2;
uniform float u_length;
uniform float u_texlen;

varying vec4 v_color;
varying vec2 v_texCoords;

void main(){
    vec2 offset = (u_uv2 - u_uv);
    vec2 coords = (v_texCoords - u_uv) / offset;
    float u = mod((coords.x * (u_length / u_texlen)), 1.0);
    vec2 ucoords = vec2(mix(u_uv.x, u_uv2.x, u), v_texCoords.y);

    vec4 c = texture2D(u_texture, ucoords);

    gl_FragColor = c * v_color;
}
