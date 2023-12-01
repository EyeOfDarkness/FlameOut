package flame.effects;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.*;
import flame.entities.*;
import flame.graphics.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class BloodSplatter extends DrawEntity implements Poolable{
    float x1, y1, x2, y2;
    float length;
    float angle;
    float size;
    float time = 0f, lifetime = 0f, lifetime2;
    float randColor = 1f;
    //Color color = FlamePal.blood;
    Color color = new Color(FlamePal.blood);
    //PowOut out = new PowOut(2);
    DynamicPowOut out = new DynamicPowOut();

    static float vx = 0f, vy = 0f;
    static float baseLifetime = 15f * 60f;

    public static void setVelocity(){
        vx = vy = 0f;
    }
    public static void setVelocity(float x, float y){
        vx = x;
        vy = y;
    }
    public static void setVelocity(Vec2 v){
        setVelocity(v.x, v.y);
    }
    
    public static void setLifetime(float life){
        baseLifetime = life;
    }
    public static void setLifetime(){
        baseLifetime = 15f * 60f;
    }

    public static void explosion(int amount, float x, float y, float boundary, float length, float size){
        explosion(amount, x, y, boundary, length, size, 35f, FlamePal.blood, 0.2f);
    }

    public static void explosion(int amount, float x, float y, float boundary, float length, float size, float time, Color color, float colorRand){
        for(int i = 0; i < amount; i++){
            float rb = Mathf.random(boundary);
            float rlen = Mathf.random(length);
            float rot = Mathf.random(360f);

            Vec2 v = Tmp.v1.trns(rot + Mathf.range(15f), rb).add(x, y);
            Vec2 v2 = Tmp.v2.trns(rot, rlen + rb).add(x, y);

            create(v.x, v.y, v2.x, v2.y, (rlen / 500) * time * Mathf.random(0.7f, 1.3f), (size * Mathf.random(0.5f, 1.5f)) / (1 + rlen / length), color, colorRand);
        }
    }
    
    public static void directionalExplosion(int amount, float x, float y, float rotation, float spread, float boundary, float length, float size, Color color, float colorRand){
        for(int i = 0; i < amount; i++){
            float rb = Mathf.random(boundary);
            float r = Mathf.pow(Mathf.random(), 1.5f);
            float rlen = Mathf.random(length * (1.5f - r));
            float rot2 = Mathf.random(360f);
            float rot = (r * Mathf.randomSign() * spread) + rotation;

            Vec2 v = Tmp.v1.trns(rot2, rb).add(x, y);
            Vec2 v2 = Tmp.v2.trns(rot, rlen).add(v.x, v.y);

            create(v.x, v.y, v2.x, v2.y, (rlen / 500) * 35f * Mathf.random(0.7f, 1.3f), (size * Mathf.random(0.5f, 1.5f)) / (1 + rlen / length), color, colorRand);
        }
    }

    public static void create(float x, float y, float x2, float y2, float lifetime, float size){
        create(x, y, x2, y2, lifetime, size, FlamePal.blood, 0.2f);
    }

    public static void create(float x, float y, float x2, float y2, float lifetime, float size, Color color, float colorRand){
        BloodSplatter b = Pools.obtain(BloodSplatter.class, BloodSplatter::new);
        b.x1 = x;
        b.y1 = y;
        b.x2 = x2 + vx * lifetime;
        b.y2 = y2 + vy * lifetime;
        b.size = size;

        b.x = (x + x2) / 2f;
        b.y = (y + y2) / 2f;

        b.angle = Angles.angle(x, y, x2, y2);
        b.length = Mathf.dst(x, y, x2, y2);
        //b.randColor = Mathf.random(0.8f, 1.2f);
        b.color.set(color);
        b.randColor = 1 + Mathf.range(colorRand);
        b.lifetime2 = lifetime;
        b.lifetime = Math.max((lifetime / 3) + baseLifetime + Mathf.random(60f), b.lifetime2 + 120f);
        b.out.power = Mathf.random(1f, 2f);

        b.add();
    }

    @Override
    public void update(){
        time += Time.delta;
        if(time > lifetime){
            time = lifetime;
            remove();
        }
    }

    @Override
    public void draw(){
        Tmp.c1.set(color).mul(Mathf.lerp(randColor, 1f, Mathf.curve(time / lifetime, 0f, 0.5f)));
        //Draw.color(Tmp.c1);
        float z = Draw.z();
        //Draw.z(Layer.floor + 0.001f);
        float life = lifetime2;
        float fin1 = Mathf.clamp(time / life);
        float fin2 = Mathf.clamp((time - life / 1.75f) / life);

        Draw.z(Layer.flyingUnitLow);
        if(fin2 < 1){
            Draw.color(Tmp.c2.set(Tmp.c1).mul(1.1f - 0.1f * Interp.pow4In.apply(fin1)));
            float width = (4f + size / 10f) * (1 + out.apply(1 - fin1));
            float out2 = 1f - Mathf.curve(fin2, 0.9f, 1f);

            Tmp.v1.set(x1, y1).lerp(x2, y2, out.apply(fin1));
            Tmp.v2.set(x1, y1).lerp(x2, y2, out.apply(fin2));

            GraphicUtils.tri(Tmp.v1.x, Tmp.v1.y, Tmp.v2.x, Tmp.v2.y, width * out2, angle);
            Drawf.tri(Tmp.v1.x, Tmp.v1.y, width * out2, width * 2f, angle);
            //Fill.circle(Tmp.v1.x, Tmp.v1.y, width);
        }

        Draw.color(Tmp.c1);
        float offz = (id % 16) * 0.0001f;
        Draw.z((Layer.debris - 1f) + offz);
        if(time > life * 0.9f){
            //float fin3 = Mathf.clamp((time - life * 0.9f) / (lifetime - (life * 0.9f)));
            float timeOff = time - life * 0.9f;
            float fout = 1f - Mathf.curve(Mathf.clamp(timeOff / (lifetime - (life * 0.9f))), 0.5f, 1f);
            float fin4 = Mathf.clamp((time - life * 0.9f) / 10f);
            //float len = Mathf.pow(length * 0.4f, 1f / out.power);
            float len = Mathf.pow(length * ((lifetime - lifetime2) / lifetime), 1f / out.power);
            int count = (int)(len / 10);

            Rand r = Utils.rand;
            r.setSeed(id);

            Fill.circle(x2, y2, fin4 * fout * size);
            float s = size * 0.75f;

            for(int i = 0; i < count; i++){
                float t = Mathf.clamp((timeOff - i * 8f) / 15f);
                float l = i / (float)count;

                Vec2 v = Tmp.v1.trns(angle + r.range(10f), r.random(len * l, len * (l + 1))).add(x2, y2);
                Fill.circle(v.x, v.y, s * t * fout * r.random(0.9f, 1.1f));

                s *= 0.6f;
            }
        }

        Draw.z(z);
    }

    @Override
    public float clipSize(){
        return length * 2f;
    }

    @Override
    protected void removeGroup(){
        super.removeGroup();
        Groups.queueFree(this);
    }

    @Override
    public void reset(){
        x1 = y1 = x2 = y2 = 0f;
        length = angle = size = 0f;
        time = lifetime = lifetime2 = 0f;
        randColor = 1f;
        color.set(FlamePal.blood);
    }

    private static class DynamicPowOut implements Interp{
        float power = 1f;

        @Override
        public float apply(float a){
            return 1 - (float)Math.pow(1 - a, power);
        }
    }
}
