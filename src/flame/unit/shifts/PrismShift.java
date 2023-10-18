package flame.unit.shifts;

import arc.graphics.g2d.*;
import flame.unit.*;

import static arc.math.Interp.*;

public class PrismShift extends ShiftHandler{
    public PrismShift(String name){
        super(name, 1);
        shieldRadius = 320f;

        panels.addAll(
            new ShiftPanel(){{
                x1 = -70f;
                x2 = -4f;
                moveCurve = linear;
                widthTo = 0.6f;
                heightTo = 1f;
                widthCurve = heightCurve = a -> pow2Out.apply(slope.apply(a * a));
                
                //widthTo = 0.5f;
                //heightTo = 0.75f;
                
                u2From = u2To = 0.3f;
                
                setStartEnd(0f, 0.5f);
            }},
            new ShiftPanel(){{
                x1 = -40f;
                x2 = 0;
                moveCurve = linear;
                //widthTo = 0.4f;
                //heightTo = 0.75f;
                widthCurve = a -> pow2Out.apply(slope.apply(a * a));
                
                widthTo = 0.5f;
                heightTo = 0.75f;
                
                u2From = u2To = 0.35f;
                u1From = 0.15f;
                u1To = 0.15f;
                
                setStartEnd(0.1f, 0.7f);
            }},
            new ShiftPanel(){{
                x1 = -50f;
                x2 = -22f * 0.75f;
                moveCurve = pow2;
                widthTo = 0.4f;
                heightTo = 0.75f;
                
                u1From = 0.15f;
                u1To = 0.15f;
                
                setStartEnd(0.2f, 0.9f);
            }},
            new ShiftPanel(){{
                x1 = -69f;
                x2 = -22f * 1.1f * (1 - 0.25f);
                //widthFrom = 0.25f;
                //heightFrom = heightTo;
                //u2From = 1f;
                widthTo = heightTo = 1.1f;
                u2From = 1f;
                u2To = 0.25f;
                heightCurve = pow3Out;
                moveCurve = pow2;
                
                setStartEnd(0.4f, 0.975f);
            }},
            new ShiftPanel(){{
                x1 = -70f;
                x2 = -22f;
                
                widthFrom = 0.25f;
                heightFrom = heightTo;
                u2From = 1f;
                
                setStartEnd(0.5f, 1f);
            }}
        );
    }

    @Override
    public void drawFull(ApathyIUnit u){
        //TextureRegion c = regions[0];

        Draw.rect(u.type.region, u.x, u.y, u.rotation - 90);
    }
}
