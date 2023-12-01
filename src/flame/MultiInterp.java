package flame;

import arc.math.*;

public class MultiInterp implements Interp{
    public static Interp fastfastslow = new MultiInterp(Interp.pow2In, Interp.pow2);
    Interp[] interps;

    public MultiInterp(Interp... interps){
        this.interps = interps;
    }

    @Override
    public float apply(float v){
        for(Interp i : interps){
            v = i.apply(v);
        }

        return v;
    }
}
