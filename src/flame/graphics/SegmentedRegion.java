package flame.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

public class SegmentedRegion{
    TextureRegion region;
    int subDiv = 16;

    static float[] verts = new float[4 * 6];

    public SegmentedRegion(String name){
        region = Core.atlas.find(name);
    }

    public void render(float x, float y, float width, float height, Cons<Vec2> mover){
        TextureRegion r = region;
        float offx = width / 2f, offy = height / 2f;
        float col = Draw.getColor().toFloatBits(), mcol = Draw.getMixColor().toFloatBits();
        Vec2 t = Tmp.v1;

        for(int i = 0; i < subDiv; i++){
            float f1 = i / (float)subDiv, f2 = (i + 1f) / subDiv;

            /*
            float x1 = (width * f1 + x) - offx, x2 = (width * f2 + x) - offx;
            float y1 = (height + y) - offy, y2 = y - offy;

            float u = Mathf.lerp(r.u, r.u2, f1);
            float u2 = Mathf.lerp(r.u, r.u2, f2);
            float v = r.v2;
            float v2 = r.v;
            */
            
            float x1 = x - offx, x2 = (width + x) - offx;
            float y1 = (height * f2 + y) - offy, y2 = (height * f1 + y) - offy;

            float u = r.u;
            float u2 = r.u2;
            float v = Mathf.lerp(r.v2, r.v, f1);
            float v2 = Mathf.lerp(r.v2, r.v, f2);

            t.set(x1, y2);
            mover.get(t);
            verts[0] = t.x;
            verts[1] = t.y;
            verts[2] = col;
            verts[3] = u;
            verts[4] = v;
            verts[5] = mcol;

            t.set(x1, y1);
            mover.get(t);
            verts[6] = t.x;
            verts[7] = t.y;
            verts[8] = col;
            verts[9] = u;
            verts[10] = v2;
            verts[11] = mcol;

            t.set(x2, y1);
            mover.get(t);
            verts[12] = t.x;
            verts[13] = t.y;
            verts[14] = col;
            verts[15] = u2;
            verts[16] = v2;
            verts[17] = mcol;

            t.set(x2, y2);
            mover.get(t);
            verts[18] = t.x;
            verts[19] = t.y;
            verts[20] = col;
            verts[21] = u2;
            verts[22] = v;
            verts[23] = mcol;

            Draw.vert(region.texture, verts, 0, verts.length);
        }
    }
}
