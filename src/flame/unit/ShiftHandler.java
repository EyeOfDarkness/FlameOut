package flame.unit;

import arc.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import flame.*;

import static arc.math.Interp.*;

public class ShiftHandler{
    protected TextureRegion[] regions;
    protected int regionLength;
    protected String name;

    protected Seq<ShiftPanel> panels = new Seq<>();

    protected float lastShiftEnd = 1f;
    protected float shiftDuration = 90f;
    protected float shieldRadius = 240f;

    protected boolean stressAffected = true;
    protected Sound shiftSound = FlameSounds.transform;

    protected float[] critPoints;

    public ShiftHandler(String name, int regionLength){
        this.name = name;
        this.regionLength = regionLength;
    }

    public void load(){
        regions = new TextureRegion[regionLength];
        for(int i = 0; i < regionLength; i++){
            regions[i] = Core.atlas.find("flameout-" + name + "-" + i);
        }
        for(ShiftPanel panel : panels){
            panel.region = regions[panel.idx];
            if(panel.out != null) panel.out.region = regions[panel.idx];
        }
    }

    public void drawIn(ApathyIUnit u, float progress){
        //
        for(ShiftPanel p : panels){
            if(progress > p.renderStart && progress < p.renderEnd) p.drawIn(u, progress);
        }
    }
    public void drawOut(ApathyIUnit u, float progress){
        //drawIn(u, 1 - progress);
        for(ShiftPanel p : panels){
            if(p.renderOut) p.drawOut(u, progress);
        }
    }

    public void drawFull(ApathyIUnit u){
        for(ShiftPanel p : panels){
            if(p.renderFinish) p.drawFull(u);
        }
    }

    /** Only updates when shifting finishes */
    public void update(ApathyIUnit u){
        //
    }
    public void updateShift(ApathyIUnit u, float shift, boolean out){
        //
    }

    public static class ShiftPanel{
        public float x1, y1, x2, y2;
        public float moveStart = 0, moveEnd = 1;
        public Interp moveCurve = pow2In;

        public float widthFrom = 0, widthTo = 1;
        public float heightFrom = 0, heightTo = 1;
        public Interp widthCurve = linear, heightCurve = linear;
        public float sclStart = 0, sclEnd = 1;

        public float rotationFrom = 0f, rotationTo = 0f;
        public float rotStart = 0f, rotEnd = 1f;
        public Interp rotCurve = linear;

        public float u1From = 0f, u1To = 0f;
        public float u2From = 0f, u2To = 0f;
        public float vFrom = 0f, vTo = 0f;
        public Interp u1Curve = linear, u2Curve = linear;
        public Interp vCurve = linear;
        public float uvStart = 0, uvEnd = 1;
        
        public float renderStart = -1f, renderEnd = 2f;

        public boolean mirror = true;
        public boolean renderFinish = true, renderOut = true;
        public int idx = 0;
        protected TextureRegion region;

        public ShiftPanel out;

        public ShiftPanel(){
            //
        }
        @SuppressWarnings("CopyConstructorMissesField")
        public ShiftPanel(ShiftPanel from){
            x1 = from.x2;
            y1 = from.y2;
            widthFrom = from.widthTo;
            heightFrom = from.heightTo;

            u1From = from.u1To;
            u2From = from.u2To;
            vFrom = from.vTo;
            //v2From = from.v2To;
            mirror = from.mirror;
            
            rotationFrom = from.rotationTo;

            idx = from.idx;
        }

        public void setStartEnd(float start, float end){
            moveStart = uvStart = sclStart = rotStart = start;
            moveEnd = uvEnd = sclEnd = rotEnd = end;
        }

        public void drawIn(ApathyIUnit u, float fin){
            drawIn(u.x, u.y, u.rotation, fin);
        }
        public void drawIn(float x, float y, float rotation, float fin){
            float fs = Mathf.curve(fin, sclStart, sclEnd);
            float w = Mathf.lerp(widthFrom, widthTo, widthCurve.apply(fs));
            float h = Mathf.lerp(heightFrom, heightTo, heightCurve.apply(fs));
            if(Math.abs(w * h) <= 0.0001f) return;

            float fm = moveCurve.apply(Mathf.curve(fin, moveStart, moveEnd));
            float fuv = Mathf.curve(fin, uvStart, uvEnd);

            float u1 = u1From != u1To ? Mathf.lerp(u1From, u1To, u1Curve.apply(fuv)) : u1To;
            float u2 = u2From != u2To ? Mathf.lerp(u2From, u2To, u2Curve.apply(fuv)) : u2To;
            float vv = vFrom != vTo ? Mathf.lerp(vFrom, vTo, vCurve.apply(fuv)) : vTo;

            float rot = rotationFrom != rotationTo ? Mathf.lerp(rotationFrom, rotationTo, rotCurve.apply(Mathf.curve(fin, rotStart, rotEnd))) : rotationTo;

            //TextureRegion r = region;
            TextureRegion r = Tmp.tr1;
            r.set(region);
            float tu1 = r.u + (r.u2 - r.u) * u1;
            float tu2 = r.u2 + (r.u - r.u2) * u2;
            float vd = Math.abs(r.v2 - r.v) * vv;

            r.setU(tu1);
            r.setU2(tu2);
            r.setV(r.v + vd);
            r.setV2(r.v2 - vd);

            int mirr = mirror ? 2 : 1;
            for(int i = 0; i < mirr; i++){
                int s = i == 0 ? 1 : -1;
                Vec2 v = Tmp.v1.trns(rotation - 90f, Mathf.lerp(x1, x2, fm) * s, Mathf.lerp(y1, y2, fm));
                Draw.rect(r, x + v.x, y + v.y, r.width * Draw.scl * s * w, r.height * Draw.scl * h, (rotation - 90) + rot * s);
            }
        }

        public void drawOut(ApathyIUnit u, float f){
            if(out == null){
                drawIn(u, 1 - f);
            }else{
                out.drawIn(u, f);
            }
        }
        public void drawOut(float x, float y, float rotation, float f){
            if(out == null){
                drawIn(x, y, rotation, 1 - f);
            }else{
                out.drawIn(x, y, rotation, f);
            }
        }

        public void drawFull(ApathyIUnit u){
            drawFull(u.x, u.y, u.rotation);
        }
        public void drawFull(float x, float y, float rotation){
            float w = widthTo;
            float h = heightTo;

            //Vec2 v = Tmp.v1.trns(u.rotation - 90f, x2, y2);
            int mirr = mirror ? 2 : 1;
            for(int i = 0; i < mirr; i++){
                int s = i == 0 ? 1 : -1;
                Vec2 v = Tmp.v1.trns(rotation - 90f, x2 * s, y2);
                Draw.rect(region, x + v.x, y + v.y, region.width * Draw.scl * s * w, region.height * Draw.scl * h, (rotation - 90) + rotationTo * s);
            }
        }
    }
}
