varying lowp vec4 v_color;
varying lowp vec4 v_mix_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(){
    vec4 c = texture2D(u_texture, v_texCoords);
    
    //if(c.r <= 0.01 && c.g <= 0.01 && c.b <= 0.01) c.a = 0;
    
    //c.a = (c.a - 0.5) / 0.5;
    //c.a = (c.a - 0.8) / (1.0 - 0.8);
    //gl_FragColor = v_color * mix(c, vec4(v_mix_color.rgb, c.a), v_mix_color.a);
    float alpha = ((c.a * v_color.a) - 0.75) / (1.0 - 0.75);
    gl_FragColor = vec4(mix(c.rgb, v_mix_color.rgb, v_mix_color.a), alpha);
}
