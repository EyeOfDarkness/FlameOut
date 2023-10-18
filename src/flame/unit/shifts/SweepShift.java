package flame.unit.shifts;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import flame.unit.*;

import static arc.math.Interp.*;

public class SweepShift extends ShiftHandler{
    int arms = 10;

    public SweepShift(String name){
        super(name, 4);

        lastShiftEnd = 0.1f;
        shiftDuration = 120f;

        panels.addAll(
            new ShiftPanel(){{
                idx = 1;
                //renderOut = false;
                renderFinish = false;
                mirror = false;

                y2 = 80f;
                moveCurve = pow2Out;

                heightTo = 5f;
                //widthTo = widthFrom = 0.5f;
                widthTo = 0.5f;
                widthFrom = 0.5f;
                //widthCurve = a -> slope.apply(a * a);
                heightCurve = a -> a < 0.7f ? pow2Out.apply(a) : pow2Out.apply(0.7f) * (1 - (a - 0.7f) / 0.3f);

                setStartEnd(0.15f, 0.35f);
            }},
            new ShiftPanel(){{
                idx = 2;
                mirror = false;
                
                y1 = 90f - 5f;
                y2 = (90f - 24f * 1.25f) - 5f;
                moveCurve = pow2In;

                renderStart = 0.5f;
                setStartEnd(0.6f, 0.7f);
                
                widthFrom = widthTo = 0.75f;
                heightTo = 1.25f;
                heightCurve = pow2In;

                //widthCurve = heightCurve = pow3Out;
            }},
            new ShiftPanel(){{
                idx = 1;
                mirror = false;
                
                y1 = 80f;
                y2 = 90f;
                moveCurve = pow2Out;

                renderStart = 0.5f;
                setStartEnd(0.35f, 0.55f);

                widthCurve = heightCurve = pow3Out;
            }},
            new ShiftPanel(){{
                idx = 3;
                
                //x1 = x2 = -12f;
                x1 = -16f;
                x2 = -18f;
                
                y1 = 87f;
                y2 = 90f;
                moveCurve = pow2In;
                
                rotationFrom = 90f;
                rotCurve = pow2In;
                
                widthFrom = widthTo = 1.25f;
                //heightFrom = heightTo = 1f;
                heightCurve = pow2In;
                
                //u1From = 1f;
                //u1Curve = pow2In;
                
                setStartEnd(0.9f, 1f);
                uvEnd = 0.975f;
            }}
        );
    }

    @Override
    public void updateShift(ApathyIUnit u, float shift, boolean out){
        if(!out){
            float s = shift * shift * shift;
            u.shiftRotation += (1f + u.getStressScaled()) * s * 5f * Time.delta;
            u.shiftRotation = Mathf.mod(u.shiftRotation, 360f / arms);
        }else{
            if(u.shiftRotation > 0f){
                u.shiftRotation += (1f + u.getStressScaled()) * 5f * Time.delta;

                if(u.shiftRotation > 360f / arms){
                    u.shiftRotation = 0f;
                }
            }
        }
    }

    @Override
    public void drawIn(ApathyIUnit u, float progress){
        for(ShiftPanel p : panels){
            for(int i = 0; i < arms; i++){
                float angle = (i * (360f / arms) + 180f / arms) - (180f);
                p.drawIn(u.x, u.y, u.rotation + u.shiftRotation + angle, progress);
            }
        }
        float offset = 0.07f;
        float s = pow2In.apply(Mathf.curve(progress, 0f, offset)) * 2f;
        if(progress > offset){
            float f = (progress - offset) / (1f - offset);
            f = Mathf.curve(f, 0.1f, 0.25f);
            s = Mathf.lerp(2f, 1f, pow2In.apply(f));
        }
        TextureRegion r = regions[0];
        Draw.rect(r, u.x, u.y, r.width * Draw.scl * s, r.height * Draw.scl * s, u.rotation - 90f);
    }

    @Override
    public void drawFull(ApathyIUnit u){
        for(ShiftPanel p : panels){
            if(!p.renderFinish) continue;
            for(int i = 0; i < arms; i++){
                float angle = (i * (360f / arms) + 180f / arms) - (180f);
                p.drawFull(u.x, u.y, u.rotation + u.shiftRotation + angle);
            }
        }

        Draw.rect(regions[0], u.x, u.y, u.rotation - 90f);
    }

    @Override
    public void drawOut(ApathyIUnit u, float progress){
        float af = pow2In.apply(Mathf.curve(progress, 0.2f, 0.75f));

        for(ShiftPanel p : panels){
            for(int i = 0; i < arms; i++){
                if(!p.renderOut) continue;
                float angle = ((i * (360f / arms) + 180f / arms) - (180f)) * (1 - af);
                p.drawOut(u.x, u.y, u.rotation + u.shiftRotation + angle, progress);
            }
        }

        TextureRegion r = regions[0];
        float fin = 1f - pow2Out.apply(progress);
        Draw.rect(r, u.x, u.y, r.width * Draw.scl * fin, r.height * Draw.scl * fin, u.rotation - 90f);
    }
}
