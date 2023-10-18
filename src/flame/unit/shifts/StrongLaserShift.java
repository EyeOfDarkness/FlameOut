package flame.unit.shifts;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.unit.*;
import mindustry.gen.*;

import static arc.math.Interp.*;

public class StrongLaserShift extends ShiftHandler{
    public StrongLaserShift(String name){
        super(name, 1);

        shiftDuration = 4f * 60f;
        lastShiftEnd = 0.25f;
        stressAffected = false;
        shiftSound = FlameSounds.largeTransform;
        
        critPoints = new float[]{
            0f, 20f
        };

        panels.addAll(
            new StrongLaserPanel(){{
                //x1 = x2 = -30f;
                x1 = -30f;
                x2 = -30f;
                y1 = 0f;
                y2 = 40f;
                
                offsetX = 23.75f;
                offsetY = -62f;

                rotationFrom = 0f;
                rotationTo = 30f;
                
                widthCurve = pow2In;
                heightCurve = pow3In;
                
                //moveCurve = rotCurve = a -> pow2.apply(a * a);
                moveCurve = rotCurve = pow2;
                
                setStartEnd(0.5f, 1f);
                //sclEnd -= 0.125f;
                sclEnd = sclStart + 0.135f;
                
                //sclEnd -= 0.5f;
                out = new StrongLaserPanel(this){{
                    widthCurve = heightCurve = pow3In;

                    widthTo = heightTo = 0f;
                }};
            }},
            new StrongLaserPanel(){{
                //x1 = x2 = -30f;
                x1 = -20f;
                x2 = -45f;
                y1 = -25f;
                y2 = 0f;
                
                offsetX = 23.75f;
                offsetY = -62f;

                rotationFrom = 0f;
                rotationTo = 70f;
                
                widthCurve = pow2In;
                heightCurve = pow3In;
                
                widthTo = 0.75f;
                heightTo = 0.75f;
                
                moveCurve = rotCurve = a -> pow2.apply(pow2Out.apply(a));
                //moveCurve = rotCurve = pow3;
                
                setStartEnd(0.25f, 1f);
                //sclStart = 0.4f;
                sclEnd = sclStart + 0.135f;

                out = new StrongLaserPanel(this){{
                    widthCurve = heightCurve = pow3In;

                    widthTo = heightTo = 0f;
                }};
                
                //sclEnd -= 0.5f;
            }},
            new StrongLaserPanel(){{
                //x1 = x2 = -30f;
                x1 = 0f;
                x2 = -45f;
                y1 = 0f;
                y2 = -30f;
                
                offsetX = 23.75f;
                offsetY = -62f;

                rotationFrom = 0f;
                rotationTo = 110f;
                
                widthCurve = pow2In;
                heightCurve = pow3In;
                
                widthTo = 0.5f;
                heightTo = 0.5f;
                
                moveCurve = rotCurve = a -> pow2.apply(pow2Out.apply(a));
                //moveCurve = rotCurve = pow3;
                
                setStartEnd(0f, 1f);
                //sclStart = 0.45f;
                sclEnd = sclStart + 0.135f;

                out = new StrongLaserPanel(this){{
                    widthCurve = heightCurve = pow3In;

                    widthTo = heightTo = 0f;
                }};
                
                //sclEnd -= 0.5f;
            }}
        );
    }

    static class StrongLaserPanel extends ShiftPanel{
        public float offsetX = 0f, offsetY = 0f;

        StrongLaserPanel(){
            super();
        }

        StrongLaserPanel(StrongLaserPanel panel){
            super(panel);
            offsetX = panel.offsetX;
            offsetY = panel.offsetY;
        }

        @Override
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
                Vec2 v2 = Tmp.v2.trns(rot * s + rotation - 90f, offsetX * s * w, offsetY * h);
                Draw.rect(r, x + v.x - v2.x, y + v.y - v2.y, r.width * Draw.scl * s * w, r.height * Draw.scl * h, (rotation - 90) + rot * s);
            }
        }

        @Override
        public void drawFull(float x, float y, float rotation){
            float w = widthTo;
            float h = heightTo;

            //Vec2 v = Tmp.v1.trns(u.rotation - 90f, x2, y2);
            int mirr = mirror ? 2 : 1;
            for(int i = 0; i < mirr; i++){
                int s = i == 0 ? 1 : -1;
                Vec2 v = Tmp.v1.trns(rotation - 90f, x2 * s, y2);
                Vec2 v2 = Tmp.v2.trns(rotationTo * s + rotation - 90f, offsetX * s * w, offsetY * h);
                Draw.rect(region, x + v.x - v2.x, y + v.y - v2.y, region.width * Draw.scl * s * w, region.height * Draw.scl * h, (rotation - 90) + rotationTo * s);
            }
        }
    }
}
