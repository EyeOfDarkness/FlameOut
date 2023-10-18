package flame.unit.shifts;

import flame.unit.*;

import static arc.math.Interp.*;

public class WeakLaserShift extends ShiftHandler{
    public WeakLaserShift(String name){
        super(name, 3);

        critPoints = new float[]{
            0f, 30f,
            180f, 30f
        };

        panels.addAll(
            new ShiftPanel(){{
                idx = 1;
                renderFinish = false;
                
                x1 = 0f;
                x2 = 75f;
                moveCurve = linear;
                vFrom = 0.5f;
                vCurve = pow2Out;
                //widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                widthCurve = a -> slope.apply(pow2Out.apply(a));
                heightCurve = pow3Out;
                
                //widthTo = heightTo = 1.1f;
                heightTo = 1f;
                widthTo = 0.5f;
                
                setStartEnd(0f, 0.4f);
                uvEnd = 0.3f;
                
                out = new ShiftPanel(this){{
                    x1 = 6f;
                    x2 = 65f;
                    moveCurve = pow2In;
                    
                    heightFrom = 0.5f;
                    widthFrom = 0f;
                    widthTo = 0.5f;
                    heightTo = 1.5f;
                    //vTo = 0.5f;
                    vFrom = 0.5f;
                    vCurve = pow3Out;
                    
                    widthCurve = a -> slope.apply(pow2Out.apply(a));
                    
                    setStartEnd(0.5f, 1f);
                }};
            }},
            new ShiftPanel(){{
                idx = 1;
                renderFinish = false;
                
                x1 = 0f;
                x2 = 60f;
                moveCurve = linear;
                vFrom = 0.5f;
                vCurve = pow2Out;
                //widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                widthCurve = a -> slope.apply(pow2Out.apply(a));
                heightCurve = pow3Out;
                
                //widthTo = heightTo = 1.1f;
                heightTo = 1.25f;
                widthTo = 0.75f;
                
                setStartEnd(0.1f, 0.5f);
                uvEnd = 0.4f;
                
                out = new ShiftPanel(this){{
                    x1 = 6f;
                    x2 = 80f;
                    moveCurve = pow2In;
                    
                    heightFrom = 0.5f;
                    widthFrom = 0f;
                    widthTo = 0.75f;
                    heightTo = 1.25f;
                    //vTo = 0.5f;
                    vFrom = 0.5f;
                    vCurve = pow3Out;
                    
                    widthCurve = a -> slope.apply(pow2Out.apply(a));
                    
                    setStartEnd(0.35f, 0.95f);
                }};
            }},
            new ShiftPanel(){{
                idx = 1;
                renderFinish = false;
                
                x1 = 2f;
                x2 = 50f;
                moveCurve = linear;
                vFrom = 0.5f;
                vCurve = pow2Out;
                //widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                widthCurve = a -> slope.apply(pow2Out.apply(a));
                heightCurve = pow3Out;
                
                //widthTo = heightTo = 1.1f;
                heightTo = 1.5f;
                widthTo = 1f;
                
                setStartEnd(0.2f, 0.6f);
                uvEnd = 0.5f;
                
                out = new ShiftPanel(this){{
                    x1 = 0f;
                    x2 = 90f;
                    moveCurve = pow2In;
                    
                    heightFrom = 0.5f;
                    widthTo = 1f;
                    widthFrom = 0f;
                    //vTo = 0.5f;
                    vFrom = 0.5f;
                    vCurve = pow3Out;
                    
                    widthCurve = a -> slope.apply(pow2Out.apply(a));
                    
                    setStartEnd(0.2f, 0.9f);
                }};
            }},
            
            new ShiftPanel(){{
                idx = 1;
                
                x1 = -10f;
                x2 = 30f;
                moveCurve = pow2Out;
                vFrom = 0.5f;
                vCurve = pow2In;
                widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                
                //widthTo = heightTo = 1.1f;
                heightTo = 0.9f;
                widthTo = 0.9f;
                
                setStartEnd(0.55f, 1f);
                sclEnd = uvEnd = 0.8f;
                
                out = new ShiftPanel(this){{
                    x2 = 130f;
                    
                    vTo = 0.5f;
                    
                    widthTo = 0f;
                    heightTo = 2f;
                    
                    widthCurve = pow3Out;
                    heightCurve = pow2In;
                    
                    setStartEnd(0.5f, 1f);
                }};
            }},
            new ShiftPanel(){{
                idx = 1;
                
                x1 = -10f;
                x2 = 35f;
                moveCurve = pow2Out;
                vFrom = 0.5f;
                vCurve = pow2In;
                widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                
                //widthTo = heightTo = 1.1f;
                heightTo = 1.25f;
                
                setStartEnd(0.4f, 0.9f);
                sclEnd = uvEnd = 0.7f;
                
                out = new ShiftPanel(this){{
                    x2 = 120f;
                    
                    vTo = 0.25f;
                    
                    widthTo = 0f;
                    heightTo = 2f;
                    
                    widthCurve = pow3Out;
                    heightCurve = pow2In;
                    
                    setStartEnd(0.4f, 0.85f);
                }};
            }},
            new ShiftPanel(){{
                x1 = -10f;
                x2 = 50f;
                //moveCurve = linear;
                moveCurve = pow2Out;
                
                vFrom = 0.5f;
                vCurve = pow2In;
                
                widthFrom = 0.0f;
                widthCurve = a -> pow2.apply(a * a);
                heightFrom = 0.5f;
                
                setStartEnd(0.25f, 0.75f);
                sclEnd = uvEnd = 0.6f;
                //sclStart = 0.10f;
                
                //sclEnd = 0.5f;
                
                out = new ShiftPanel(this){{
                    x2 = 90f;
                    
                    vTo = 0.5f;
                    
                    widthTo = 0f;
                    heightTo = 0.5f;
                    
                    widthCurve = pow3Out;
                    
                    setStartEnd(0.2f, 0.7f);
                }};
            }},
            new ShiftPanel(){{
                idx = 2;
                
                x1 = 35f;
                x2 = 45f;
                moveCurve = pow2Out;
                
                //heightFrom = 1.2f;
                vFrom = 0.5f;
                vCurve = pow2Out;
                
                widthTo = 0.75f;
                heightTo = 1.2f;
                
                setStartEnd(0.85f, 1f);
                sclEnd = 0.9f;
                
                out = new ShiftPanel(this){{
                    x2 = 90f;
                    
                    vTo = 0.5f;
                    
                    widthTo = 0f;
                    heightTo = 2.5f;
                    
                    setStartEnd(0f, 0.5f);
                }};
            }}
        );
    }
}
