package flame.unit.shifts;

import flame.unit.*;
import arc.math.*;

import static arc.math.Interp.*;

public class AoEShift extends ShiftHandler{
    public AoEShift(String name){
        super(name, 2);
        
        critPoints = new float[]{
            80f, 20f,
            -80f, 20f
        };

        panels.addAll(
            new ShiftPanel(){{
                mirror = false;
                
                y1 = -50f;
                
                y2 = -82.5f;
                moveCurve = pow2Out;

                //rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                heightTo = 0.3375f;
                widthTo = 0.4921875f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0.5f, 1f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 0.3375f;
                    widthFrom = widthTo = 0.4921875f;
                    
                    //rotationTo = rotationFrom + 90f;
                    //rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0f, 0.55f);
                }};
            }},
            new ShiftPanel(){{
                mirror = false;
                
                y1 = -40f;
                
                y2 = -72.5f;
                moveCurve = pow2Out;

                //rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                heightTo = 0.45f;
                widthTo = 0.65625f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0.4f, 0.9f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 0.45f;
                    widthFrom = widthTo = 0.65625f;
                    
                    //rotationTo = rotationFrom + 90f;
                    //rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0.1f, 0.65f);
                }};
            }},
            new ShiftPanel(){{
                mirror = false;
                
                y1 = -20f;
                
                y2 = -60f;
                moveCurve = pow2Out;

                //rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                heightTo = 0.6f;
                widthTo = 0.875f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0.3f, 0.8f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 0.6f;
                    widthFrom = widthTo = 0.875f;
                    
                    //rotationTo = rotationFrom + 90f;
                    //rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0.2f, 0.75f);
                }};
            }},

            new ShiftPanel(){{
                x1 = 50f;
                y1 = -50f;
                
                x2 = 60f;
                y2 = -60f;
                moveCurve = pow2Out;

                rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                heightTo = 0.5625f;
                widthTo = 0.5625f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0.2f, 0.7f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    x2 = x1 + 15f;
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 0.5625f;
                    widthFrom = widthTo = 0.5625f;
                    
                    rotationTo = rotationFrom - 90f;
                    rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0.3f, 0.8f);
                }};
            }},
            new ShiftPanel(){{
                x1 = 40f;
                y1 = -40f;
                
                x2 = 50f;
                y2 = -50f;
                moveCurve = pow2Out;

                rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                heightTo = 0.75f;
                widthTo = 0.75f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0.1f, 0.6f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    x2 = x1 + 15f;
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 0.75f;
                    widthFrom = widthTo = 0.75f;
                    
                    rotationTo = rotationFrom - 90f;
                    rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0.4f, 0.9f);
                }};
            }},
            new ShiftPanel(){{
                x1 = 20f;
                y1 = -20f;
                
                x2 = 40f;
                y2 = -40f;
                moveCurve = pow2Out;

                rotationFrom = rotationTo = 45f;

                heightCurve = pow2Out;
                widthCurve = pow2Out;
                //sclEnd = 0.5f;
                vFrom = 0.5f;
                
                vCurve = pow2Out;
                //widthCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                setStartEnd(0, 0.5f);
                sclEnd -= 0.25f;
                
                out = new ShiftPanel(this){{
                    x2 = x1 + 15f;
                    y2 = y1 - 15f;
                    
                    heightFrom = heightTo = 1f;
                    widthFrom = widthTo = 1f;
                    
                    rotationTo = rotationFrom - 90f;
                    rotCurve = pow3In;
                    
                    vTo = 0.5f;
                    
                    setStartEnd(0.5f, 1f);
                }};
            }},
            
            new ShiftPanel(){{
                idx = 1;
                renderFinish = false;
                //renderOut = false;
                
                x1 = -15f;
                
                x2 = -25f;
                y2 = 40f;
                vFrom = 0.5f;
                
                //heightCurve = a -> pow2Out.apply(slope.apply(a * a));
                //widthCurve = pow4Out;
                vCurve = a -> pow2Out.apply(slope.apply(a * a));
                
                //widthFrom = heightFrom = 0.6f;
                widthFrom = 1f;
                //heightFrom = heightTo = -0.6f;
                heightFrom = -0.6f;
                heightTo = 0.6f;
                widthTo = 0.6f;
                
                heightCurve = pow3;
                
                setStartEnd(0.2f, 0.6f);
                
                out = new ShiftPanel(this){{
                    x1 = -25f;
                    y1 = 60f;
                    
                    x2 = -25f;
                    y2 = 95f;
                    
                    widthFrom = heightFrom = 0f;
                    widthTo = heightTo = 0.75f;
                    //widthCurve = heightCurve = pow4Out;
                    widthCurve = a -> pow2Out.apply(slope.apply(a));
                    heightCurve = a -> pow2Out.apply(Mathf.curve(a, 0f, 0.5f));
                    vFrom = 0f;
                    vTo = 0f;
                    
                    //vFrom = 0.5f;
                    //vCurve = a -> pow2Out.apply(slope.apply(a * a));
                    
                    setStartEnd(0.15f, 0.65f);
                }};
            }},
            new ShiftPanel(){{
                idx = 1;
                
                x1 = -25f;
                
                x2 = -25f;
                y2 = 60f;
                
                moveCurve = pow2Out;
                heightCurve = pow2Out;
                
                widthFrom = 0.9f;
                
                vFrom = 0.5f;
                vCurve = pow2Out;
                
                setStartEnd(0.5f, 1f);
                
                sclEnd -= 0.25f;
                moveEnd -= 0.25f;

                out = new ShiftPanel(this){{
                    x2 = -25f;
                    y2 = 95f;
                    moveCurve = pow2Out;

                    //heightTo = 0f;
                    vTo = 0.5f;
                    vCurve = pow2Out;

                    widthCurve = heightCurve = pow4In;
                    
                    setStartEnd(0f, 0.5f);
                }};
            }}
        );
    }
}
