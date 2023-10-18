uniform sampler2D u_texture;
uniform vec2 u_viewport;
uniform vec2 u_position;
uniform float u_radius;
uniform float u_scl;
uniform float u_alpha;

varying vec2 v_texCoords;

void main(){
    vec2 pos = (v_texCoords * u_viewport) - u_position;
    float len = (length(pos) - u_radius) / u_scl;

    if(len < 0.0 && len > -1.0){
        //vec4 nc = texture2D(u_texture, v_texCoords - off);
        vec4 nc = texture2D(u_texture, vec2(-len, 0.5));
        nc.a = nc.a * u_alpha;
        gl_FragColor = nc;
        return;
    }

    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
}
