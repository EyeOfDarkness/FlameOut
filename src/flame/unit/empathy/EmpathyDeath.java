package flame.unit.empathy;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.graphics.*;
import flame.unit.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static arc.math.Interp.*;

public class EmpathyDeath{
    float time = 0f;
    float x, y, rotation;

    static float duration = 5f * 60;
    static float[] speeds = {1f, -0.75f, 0.5f};

    void update(){
        time += Time.delta;
    }
    void draw(){
        UnitType type = FlameUnitTypes.empathy;
        float z = Layer.flyingUnitLow;
        float lz = Draw.z();
        float fout = Mathf.clamp((duration - time) / 7f);
        float fout2 = Mathf.clamp((duration - time) / 90f);

        TextureRegion r = type.region;

        float scl = Draw.scl * fout;

        float glow = Mathf.clamp(time / 240f);
        float glow2 = Mathf.clamp((time - 60f) / 240f);
        float chains = Mathf.clamp((time - 20f) / 100f);
        float interf = Mathf.clamp((time - 120f) / 120f);
        float portal = pow2.apply(Mathf.clamp((time - 130f) / 140f));

        Draw.z(z);
        if(portal > 0){
            float sin = 1f + Mathf.absin(1f, 0.1f);
            Draw.color(Color.black);
            GraphicUtils.diamond(x, y, 60f * portal * pow2In.apply(fout) * sin, 60f * portal * sin * (1f + pow2In.apply(1f - fout) * 3f), 0f);
            Draw.color();
        }

        if(interf > 0){
            int count = 0;

            Draw.blend(Blending.additive);
            for(TextureRegion tr : EmpathyRegions.endAPI){
                float c = 0.4f;
                float c1 = count / 2f;
                float cf = pow2.apply(Mathf.curve(interf, c * c1, (1f - c) + c * c1));
                Draw.color(FlamePal.red, cf * fout);
                Draw.rect(tr, x, y, tr.width * Draw.scl * 2f, tr.height * Draw.scl * 2f, time * speeds[count]);

                count++;
            }
            Draw.blend();
        }

        if(chains > 0 && chains < 1){
            //float fin1 = pow2In.apply(Mathf.clamp(chains * 8f));
            float fin1 = (Mathf.clamp(chains * 6f));
            for(int i = 0; i < 16; i++){
                float escl = Mathf.slope(((i / 16f) * 4) % 1f);
                float ang = (360f / 16) * i;
                float c = 0.3f;
                float cfin = pow2In.apply(Mathf.curve(fin1, c * escl, (1f - c) + c * escl));
                Tmp.v1.trns(ang, 150f + 300f * pow2In.apply(escl)).add(x, y);
                Tmp.v2.set(Tmp.v1).lerp(x, y, cfin);
                Tmp.c1.set(FlamePal.red).a(1f - Mathf.curve(chains, 0.5f, 1f));
                GraphicUtils.chain(Tmp.v2.x, Tmp.v2.y, Tmp.v1.x, Tmp.v1.y, Tmp.c1, Blending.additive);
            }
        }

        //Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));
        //unit.type.drawSoftShadow(unit, Mathf.pow(1f - fin, 3));
        Draw.z(Math.min(Layer.darkness, z - 1f));
        //unit.type.drawShadow(unit);
        Draw.color(Pal.shadow, Pal.shadow.a * (1f - glow));
        float hx = x + UnitType.shadowTX, hy = y + UnitType.shadowTY;
        Draw.rect(r, hx, hy, rotation - 90f);

        Draw.z(Math.min(z - 0.01f, Layer.bullet - 1f));
        type.drawSoftShadow(x, y, rotation - 90f, 1f - glow);

        Draw.z(z);
        Draw.color();

        Rand rand = Utils.rand, rand2 = Utils.rand2;
        rand.setSeed(9913513L);
        float amount = 86f * glow2;
        int iam = Mathf.ceil(amount);
        for(int i = 0; i < iam; i++){
            float psc = 1f;
            if(i >= iam - 1 && glow2 < 1f){
                psc = (amount % 1f);
            }
            float dur = 40f;
            float t = (Time.time + rand.random(dur)) / dur;
            float tf = t % 1f;
            int seed = rand.nextInt() + (int)(t);
            rand2.setSeed(seed);

            float dst = rand2.random(140f, 190f) * (1f - pow2Out.apply(tf)) * pow2Out.apply(fout2);
            float rad = rand2.random(4f, 5f) * psc * tf * fout;
            float ang = rand2.random(360f);

            Vec2 v = Tmp.v1.trns(ang, dst).add(x, y);
            Fill.poly(v.x, v.y, 4, rad);
        }

        Draw.mixcol(Color.white, glow);
        Draw.rect(r, x, y, r.width * scl, r.height * scl, rotation - 90f);
        Draw.mixcol();

        Draw.z(lz);
    }
}
