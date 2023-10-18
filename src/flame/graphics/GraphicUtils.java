package flame.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.graphics.FlameShaders.*;
import mindustry.graphics.*;

public class GraphicUtils{
    static Vec3 v = new Vec3();
    static Vec2 v2 = new Vec2();
    static FloatSeq tf = new FloatSeq(4 * 2);
    static float[] verts = new float[4 * 6];
    static TextureRegion chain;

    //public static Blending invert = new Blending(Gl.oneMinusDstColor, Gl.oneMinusSrcAlpha, Gl.srcAlpha, Gl.oneMinusSrcAlpha);
    public static Blending multiply = new Blending(Gl.dstColor, Gl.oneMinusSrcAlpha, Gl.srcAlpha, Gl.oneMinusSrcAlpha);

    public static void polygram(float x, float y, float rotation, float radius, int count, int stellation){
        Lines.beginLine();
        for(int i = 0; i < count; i++){
            float r = 360f * (1f / count) * i * stellation + rotation;
            float rx = Mathf.sinDeg(r) * radius + x;
            float ry = Mathf.cosDeg(r) * radius + y;
            Lines.linePoint(rx, ry);
        }
        Lines.endLine(true);
    }

    public static void chain(float x, float y, float x2, float y2, Color color, Blending blending){
        if(chain == null || chain.texture.isDisposed()) chain = Core.atlas.find("flameout-chain");
        float r = color.r, g = color.g, b = color.b, a = color.a;

        Draw.draw(Layer.flyingUnitLow, () -> {
            ChainShader shader = FlameShaders.chainShader;
            shader.region = chain;
            shader.length = Mathf.dst(x, y, x2, y2);

            Draw.flush();
            Draw.color(Tmp.c1.set(r, g, b, a));
            Draw.blend(blending);
            Draw.shader(shader);
            Lines.stroke(chain.height * Draw.scl);
            Lines.line(chain, x, y, x2, y2, false);
            //Draw.blend();
            Draw.shader();
            Draw.blend();
            //Blending.normal.apply();
            Draw.color();
            //Draw.flush();
        });
    }

    public static void tri(float x, float y, float x2, float y2, float width, float rotation){
        float
            rx = Angles.trnsx(rotation - 90f, width / 2f),
            ry = Angles.trnsy(rotation - 90f, width / 2f);
        Fill.tri(
                x + rx, y + ry,
                x2, y2,
                x - rx, y - ry
        );
    }

    public static void diamond(float x, float y, float width, float length, float rotation){
        float tx1 = Angles.trnsx(rotation + 90f, width), ty1 = Angles.trnsy(rotation + 90f, width),
                tx2 = Angles.trnsx(rotation, length), ty2 = Angles.trnsy(rotation, length);
        Fill.quad(x + tx1, y + ty1,
                x + tx2, y + ty2,
                x - tx1, y - ty1,
                x - tx2, y - ty2);
    }

    public static void circle3D(TextureRegion region, float x, float y, float rx, float ry, float rz, float size, float angle, int iter){
        float color = Draw.getColor().toFloatBits();
        float mcolor = Color.clearFloatBits;

        for(int i = 0; i < iter; i++){
            float ang1 = (360f / iter) * i;
            float ang2 = (360f / iter) * (i + 1f);

            v2.trns(ang1, 0.5f);
            float uu1 = v2.x + 0.5f;
            float vv1 = v2.y + 0.5f;
            v2.trns(ang2, 0.5f);
            float uu2 = v2.x + 0.5f;
            float vv2 = v2.y + 0.5f;

            float mu = (region.u + region.u2) / 2f;
            float mv = (region.v + region.v2) / 2f;

            float tu1 = Mathf.lerp(region.u, region.u2, uu1), tv1 = Mathf.lerp(region.v, region.v2, vv1);
            float tu2 = Mathf.lerp(region.u, region.u2, uu2), tv2 = Mathf.lerp(region.v, region.v2, vv2);

            v2.trns(ang1 + angle, size);
            v.set(v2.x, v2.y, 0f).rotate(Vec3.Y, ry).rotate(Vec3.X, rx).rotate(Vec3.Z, rz);
            float sz1 = 700f / (700f - v.z);
            v.x *= sz1;
            v.y *= sz1;

            float x1 = v.x + x, y1 = v.y + y;

            v2.trns(ang2 + angle, size);
            v.set(v2.x, v2.y, 0f).rotate(Vec3.Y, ry).rotate(Vec3.X, rx).rotate(Vec3.Z, rz);
            float sz2 = 700f / (700f - v.z);
            v.x *= sz2;
            v.y *= sz2;

            float x2 = v.x + x, y2 = v.y + y;

            verts[0] = x;
            verts[1] = y;
            verts[2] = color;
            verts[3] = mu;
            verts[4] = mv;
            verts[5] = mcolor;

            verts[6] = x;
            verts[7] = y;
            verts[8] = color;
            verts[9] = mu;
            verts[10] = mv;
            verts[11] = mcolor;

            verts[12] = x1;
            verts[13] = y1;
            verts[14] = color;
            verts[15] = tu1;
            verts[16] = tv1;
            verts[17] = mcolor;

            verts[18] = x2;
            verts[19] = y2;
            verts[20] = color;
            verts[21] = tu2;
            verts[22] = tv2;
            verts[23] = mcolor;

            Draw.vert(region.texture, verts, 0, 24);
        }
    }

    public static void draw3D(float x, float y, float rx, float ry, float rz, Cons<Vec2> in, Floatc2 out){
        //v2.setZero();
        in.get(v2);
        v.set(v2.x, v2.y, 0f).rotate(Vec3.Y, ry).rotate(Vec3.X, rx).rotate(Vec3.Z, rz);
        float sz = 700f / (700f - v.z);
        v.x *= sz;
        v.y *= sz;
        out.get(v.x + x, v.y + y);
    }

    public static void draw3D(float x, float y, float rx, float ry, float rz, Cons<FloatSeq> drawer){
        tf.clear();
        drawer.get(tf);
        int size = tf.size;
        float[] items = tf.items;
        for(int i = 0; i < size; i += 8){
            Fill.polyBegin();
            for(int j = 0; j < 8; j += 2){
                int idx = i + j;
                float wx = items[idx];
                float wy = items[idx + 1];

                //v.set(wx, wy, 0f).rotate(Vec3.X, rx).rotate(Vec3.Y, ry).rotate(Vec3.Z, rz);
                v.set(wx, wy, 0f).rotate(Vec3.Y, ry).rotate(Vec3.X, rx).rotate(Vec3.Z, rz);
                float sz = 700f / (700f - v.z);
                v.x *= sz;
                v.y *= sz;

                //tf2.add(v.x + x, v.y + y);
                Fill.polyPoint(v.x + x, v.y + y);
            }
            Fill.polyEnd();
        }
    }

    public static void drawShockWave(float x, float y, float rx, float ry, float rz, float size, float width, int iter){
        drawShockWave(x, y, rx, ry, rz, size, width, iter, 0.02f);
    }

    public static void drawShockWave(float x, float y, float rx, float ry, float rz, float size, float width, int iter, float zRange){
        float off = (360f / iter);
        //float scl = size + width;
        float zz = Draw.z();

        for(int i = 0; i < iter; i++){
            float angle1 = off * i;
            float angle2 = off * (i + 1);
            float z = 0f;

            tf.clear();
            for(int j = 0; j < 4; j++){
                float w = j == 0 || j == 3 ? width : -width;
                float a = j <= 1 ? angle1 : angle2;

                v2.trns(a, size + w);
                v.set(v2.x, v2.y, 0f).rotate(Vec3.X, rx).rotate(Vec3.Y, ry).rotate(Vec3.Z, rz);
                float sz = 700f / (700f - v.z);
                v.x *= sz;
                v.y *= sz;
                //v.add(x, y, 0f);

                z += v.z;
                tf.add(v.x + x, v.y + y);
            }
            //float tz = Mathf.clamp((z / 4f) / sizeDepth) * zRange + zz;
            float tz = (z < 0f ? -zRange : zRange) + zz;
            Draw.z(tz);
            Fill.polyBegin();
            for(int j = 0; j < 4; j++){
                float vx = tf.items[j * 2];
                float vy = tf.items[j * 2 + 1];
                Fill.polyPoint(vx, vy);
            }
            Fill.polyEnd();
        }
        Draw.z(zz);
    }

    public static void drawDeath(TextureRegion region, int seed, float x, float y, float rotation, float deathTime, float deathTime2){
        float fout1 = 1 - Mathf.curve(deathTime, 0.1f, 0.35f);
        //float fc = 1 / 2f;
        float fc = 0.75f;
        float fc2 = 1 - fc;
        float minUv = 0.125f;

        Rand r = Utils.rand, r2 = Utils.rand2;
        r.setSeed(seed);
        Color outC = FlamePal.blood;

        Draw.color(Color.white, outC, deathTime2);

        float color = Draw.getColor().toFloatBits();
        Tmp.c1.set(Color.white).lerp(outC, Interp.pow2Out.apply(deathTime2));
        float color2 = Tmp.c1.toFloatBits();
        Tmp.c1.set(Color.white).lerp(outC, Interp.pow3Out.apply(deathTime2));
        float color3 = Tmp.c1.toFloatBits();
        
        float mcolor = Draw.getMixColor().toFloatBits();

        Draw.rect(region, x, y, region.width * Draw.scl * fout1, region.height * Draw.scl * fout1, rotation - 90f);
        for(int i = 0; i < 50; i++){
            float coff = ((1f - (i / 50f)) + r.range(0.125f) + 0.125f) / 1.25f;
            float ff = coff * fc;
            float f = Mathf.curve(deathTime, ff, Mathf.clamp(ff + fc2 + r.range(0.05f), ff + 0.125f, 1f));
            float size = r.random(0.75f, 1.5f) * Mathf.lerp(300f, 110f, (i / 50f));
            int nseed = r.nextInt();
            if(f > 0.001f){
                tf.clear();
                r2.setSeed(nseed);
                float angle = r2.random(360f);

                //float x1 = r2.random(0.1f, 0.325f);
                //float y1 = r2.random(0f, 0.7f);

                //float x2 = -r2.random(0.1f, 0.325f);
                //float y2 = r2.random(0f, 0.7f);

                for(int j = 0; j < 4; j++){
                    float dx = x;
                    float dy = y;
                    float c = color;
                    switch(j){
                        case 1 -> {
                            float y1 = r2.random(0.1f, 0.325f);
                            float x1 = r2.random(0f, 0.7f);
                            v2.trns(angle, x1 * size * f, y1 * size * f);
                            dx = v2.x + x;
                            dy = v2.y + y;
                            c = color2;
                        }
                        case 2 -> {
                            v2.trns(angle, size * f);
                            dx = v2.x + x;
                            dy = v2.y + y;
                            c = color3;
                        }
                        case 3 -> {
                            float y1 = -r2.random(0.1f, 0.325f);
                            float x1 = r2.random(0f, 0.7f);
                            v2.trns(angle, x1 * size * f, y1 * size * f);
                            dx = v2.x + x;
                            dy = v2.y + y;
                            c = color2;
                        }
                    }
                    float u = Mathf.lerp(region.u, region.u2, r2.random(minUv, 1 - minUv));
                    float v = Mathf.lerp(region.v, region.v2, r2.random(minUv, 1 - minUv));
                    tf.addAll(dx, dy, c, u, v, mcolor);
                }
                Draw.vert(region.texture, tf.items, 0, tf.size);
            }
        }
        Draw.color();
    }
}
