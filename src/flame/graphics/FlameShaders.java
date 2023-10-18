package flame.graphics;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.Time;
import flame.*;

import static mindustry.Vars.*;

public class FlameShaders{
    public static HarshShadowShader harshShadow;
    public static AlphaCut alphaCut;
    public static ChainShader chainShader;
    public static PinkShader pinkShader;
    public static BlackHoleShader blackholeShader;
    public static ChaosShader chaosShader;
    public static OrderShader orderShader;
    public static ThirdImpactShader thirdImpactShader;

    static String defaultVert = """
                    attribute vec4 a_position;
                    attribute vec4 a_color;
                    attribute vec2 a_texCoord0;
                    attribute vec4 a_mix_color;
                    uniform mat4 u_projTrans;
                    varying vec4 v_color;
                    varying vec4 v_mix_color;
                    varying vec2 v_texCoords;

                    void main(){
                       v_color = a_color;
                       v_color.a = v_color.a * (255.0/254.0);
                       v_mix_color = a_mix_color;
                       v_mix_color.a *= (255.0/254.0);
                       v_texCoords = a_texCoord0;
                       gl_Position = u_projTrans * a_position;
                    }""";

    public static void load(){
        harshShadow = new HarshShadowShader();
        alphaCut = new AlphaCut();
        chainShader = new ChainShader();
        pinkShader = new PinkShader();
        blackholeShader = new BlackHoleShader();
        chaosShader = new ChaosShader();
        orderShader = new OrderShader();
        thirdImpactShader = new ThirdImpactShader();
    }

    public static Fi file(String path){
        return tree.get("shaders/" + path);
    }
    public static Fi intFile(String path){
        return Core.files.internal("shaders/" + path);
    }

    public static class AlphaCut extends Shader{
        AlphaCut(){
            super(defaultVert, file("alphacut.frag").readString());
        }
    }

    public static class HarshShadowShader extends Shader{
        static final int maxLights = 16 * 3;
        FloatSeq lights = new FloatSeq(maxLights), uniforms = new FloatSeq(maxLights);

        HarshShadowShader(){
            super(intFile("screenspace.vert"), file("harshshadow.frag"));
        }

        public void clear(){
            lights.clear();
        }
        public void addLight(float x, float y, float intensity){
            if(lights.size < maxLights){
                lights.add(x, y, intensity);
            }
        }

        @Override
        public void apply(){
            FrameBuffer b = FlameOutSFX.inst.buffer;
            setUniformf("u_texsize", b.getWidth(), b.getHeight());
            setUniformf("u_invsize", 1f / Core.camera.width, 1f / Core.camera.height);

            uniforms.clear();

            float[] items = lights.items;
            for(int i = 0; i < lights.size; i += 3){
                Vec2 v = Core.camera.project(items[i], items[i + 1]);
                //uniforms.add(items[i], items[i + 1], items[i + 2]);
                uniforms.add(v.x, v.y, items[i + 2]);
            }

            setUniformi("u_lights_count", lights.size / 3);
            setUniform3fv("u_lights", uniforms.items, 0, uniforms.size);
        }
    }

    public static class ChainShader extends Shader{
        public TextureRegion region;
        public float length = 0f;

        ChainShader(){
            super(defaultVert, file("chain.frag").readString());
        }

        @Override
        public void apply(){
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_length", length);
            setUniformf("u_texlen", region.width * Draw.scl);
        }
    }

    public static class PinkShader extends Shader{
        PinkShader(){
            super(defaultVert, file("pinkshader.frag").readString());
        }

        @Override
        public void apply(){
            setUniformf("u_main_color", FlamePal.empathyAdd);
        }
    }

    public static class BlackHoleShader extends Shader{
        static final int maxHoles = 8 * 4;
        public FloatSeq holes = new FloatSeq(maxHoles), uniforms = new FloatSeq(maxHoles);

        BlackHoleShader(){
            super(intFile("screenspace.vert"), file("blackholeshader.frag"));
        }

        public void add(float x, float y, float intensity, float swirl){
            if(holes.size >= maxHoles || intensity <= 0) return;
            holes.add(x, y, intensity, swirl);
        }

        @Override
        public void apply(){
            FrameBuffer b = FlameOutSFX.inst.buffer;
            setUniformf("u_texsize", b.getWidth(), b.getHeight());
            setUniformf("u_invsize", 1f / Core.camera.width, 1f / Core.camera.height);
            setUniformf("u_camscl", renderer.getDisplayScale());

            uniforms.clear();

            float[] items = holes.items;
            for(int i = 0; i < holes.size; i += 4){
                Vec2 v = Core.camera.project(items[i], items[i + 1]);
                //uniforms.add(items[i], items[i + 1], items[i + 2]);
                uniforms.add(v.x, v.y, items[i + 2], items[i + 3]);
            }

            setUniformi("u_holes_count", holes.size / 4);
            setUniform4fv("u_holes", uniforms.items, 0, uniforms.size);
        }
    }

    public static class ChaosShader extends Shader{
        Texture texture;
        public TextureRegion region;
        public float width, height;
        public float srcX, srcY;

        ChaosShader(){
            super(defaultVert, file("chaosshader.frag").readString());
            texture = new Texture(file("flameout-chaos-dimension.png"));
        }

        @Override
        public void apply(){
            texture.bind(1);
            region.texture.bind(0);

            setUniformi("u_texture2", 1);
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_regionsize", width, height);
            setUniformf("u_srcpos", srcX, srcY);
            setUniformf("u_campos", Core.camera.position);
            setUniformf("u_viewport", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time);
        }
    }
    public static class OrderShader extends Shader{
        Texture texture;
        public TextureRegion region;
        public float width, height;
        public float srcX, srcY;

        OrderShader(){
            super(defaultVert, file("ordershader.frag").readString());
            texture = new Texture(file("flameout-order-dimension.png"));
        }

        @Override
        public void apply(){
            texture.bind(1);
            region.texture.bind(0);

            setUniformi("u_texture2", 1);
            setUniformf("u_uv", region.u, region.v);
            setUniformf("u_uv2", region.u2, region.v2);
            setUniformf("u_regionsize", width, height);
            setUniformf("u_srcpos", srcX, srcY);
            setUniformf("u_campos", Core.camera.position);
            setUniformf("u_viewport", Core.camera.width, Core.camera.height);
            setUniformf("u_time", Time.time);
        }
    }
    public static class ThirdImpactShader extends Shader{
        Texture texture;
        float x, y;
        float radius, scl, alpha;

        ThirdImpactShader(){
            super(intFile("screenspace.vert"), file("thirdimpactshader.frag"));
            texture = new Texture(file("flameout-impact-texture.png"));
        }

        public void draw(float z, float x, float y, float radius, float scl, float alpha){
            //Draw.blend();
            Draw.draw(z, () -> {
                this.x = x;
                this.y = y;
                this.radius = radius;
                this.scl = scl;
                this.alpha = alpha;

                Draw.flush();
                Blending.normal.apply();
                Draw.blit(texture, this);
            });
        }

        @Override
        public void apply(){
            Camera cam = Core.camera;
            
            setUniformf("u_viewport", Core.camera.width, Core.camera.height);

            //Vec2 v = Core.camera.project(x, y);
            setUniformf("u_position", (x - cam.position.x) + cam.width / 2f, (y - cam.position.y) + cam.height / 2f);
            setUniformf("u_radius", radius);
            setUniformf("u_scl", scl);
            setUniformf("u_alpha", alpha);
        }
    }
}
