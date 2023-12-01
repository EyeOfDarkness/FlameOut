package flame.effects;

import arc.math.geom.*;
import arc.struct.*;
import flame.*;
import flame.entities.*;
import mindustry.content.*;

import static arc.math.geom.Intersector.*;

public class Carve{
    static Vec2 v = new Vec2(), v2 = new Vec2();
    static float[] tmpArr = new float[8];
    static Seq<Severation> tmpSeq = new Seq<>();

    /** Was intending to carve a circle on sprites, but thinking for like an entire day about it hurts my mind. */
    public static void generate(float x, float y, float rotation, float width, Runnable drawer){
        FlameOut.cutBatch.explosionEffect = null;
        FlameOut.cutBatch.cutHandler = null;
        Seq<Severation> seq = FlameOut.cutBatch.switchBatch(drawer);

        v.trns(rotation, 1f);
        v2.trns(rotation - 90f, width);

        float dx1 = v.x * 8000f + x;
        float dy1 = v.y * 8000f + y;
        float dx2 = -v.x * 8000f + x;
        float dy2 = -v.y * 8000f + y;

        for(int i = 0; i < 2; i++){
            int offset = i * 4;
            float sign = i == 0 ? 1 : -1;

            float vx1 = dx1 + v2.x * sign;
            float vy1 = dy1 + v2.y * sign;
            float vx2 = dx2 + v2.x * sign;
            float vy2 = dy2 + v2.y * sign;

            tmpArr[offset] = vx1;
            tmpArr[offset + 1] = vy1;
            tmpArr[offset + 2] = vx2;
            tmpArr[offset + 3] = vy2;
        }

        tmpSeq.clear();
        for(int i = 0; i < 2; i++){
            int offset = i * 4;

            for(Severation s : seq){
                //if(!s.wasCut) s.cutWorld(tmpArr[offset], tmpArr[offset + 1], tmpArr[offset + 2], tmpArr[offset + 3], e -> tmpSeq.add(e));
                s.cutWorld(tmpArr[offset], tmpArr[offset + 1], tmpArr[offset + 2], tmpArr[offset + 3], e -> tmpSeq.add(e));
                tmpSeq.add(s);
            }
            if(i == 0){
                seq.clear();
                seq.addAll(tmpSeq);
            }
        }
        RenderGroupEntity.capture();
        for(Severation e : tmpSeq){
            Vec2 p = nearestSegmentPoint(dx1, dy1, dx2, dy2, e.x, e.y, v);
            if(e.isAdded() && !p.within(e.x, e.y, width)){
                e.drawRender();
            }
            e.lifetime = 0f;
            e.time = 1f;
            e.remove();
        }
        RenderGroupEntity.end();
    }
}
