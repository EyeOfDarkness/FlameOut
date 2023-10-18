package flame.effects;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import arc.util.pooling.Pool.*;
import flame.Utils.*;
import flame.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/**
 * @author EyeOfDarkness
 */
public class Disintegration implements Poolable{
    float[] xs, ys;
    int width, height;
    float drawWidth, drawHeight;
    TextureRegion region;
    int uses = 0;

    public float z = Layer.flyingUnit;
    public Color drawnColor = Color.white.cpy(), scorchColor = Pal.rubble.cpy();

    static Pool<Disintegration> pool = new BasicPool<>(Disintegration::new);
    static int maxDimension = 64;
    static Point2 tmpPoint = new Point2();
    static FloatSeq fseq = new FloatSeq();
    static int[] arr = {
            0, 0,
            1, 0,
            1, 1,
            0, 1
    };

    public Disintegration(){}

    public static Disintegration generate(TextureRegion region, float x, float y, float rotation, float width, float height, Cons<DisintegrationEntity> cons){
        int modW = (int)((5f / 320f) * Math.abs(width / Draw.scl));
        int modH = (int)((5f / 320f) * Math.abs(height / Draw.scl));
        return generate(region, x, y, rotation, width, height, modW, modH, cons);
    }

    public static Disintegration generate(TextureRegion region, float x, float y, float rotation, float width, float height, int iwidth, int iheight, Cons<DisintegrationEntity> cons){
        //int modW = (int)((20f / 320f) * Math.abs(width / Draw.scl));
        //int modH = (int)((20f / 320f) * Math.abs(height / Draw.scl));

        float cos = Mathf.cosDeg(rotation);
        float sin = Mathf.sinDeg(rotation);

        Disintegration dis = pool.obtain();
        dis.set(iwidth, iheight);
        dis.region = region;
        dis.drawWidth = width;
        dis.drawHeight = height;
        dis.uses = 0;

        int w = dis.width, h = dis.height;
        for(int ix = 0; ix < w - 1; ix++){
            for(int iy = 0; iy < h - 1; iy++){
                float ox = 0f, oy = 0f;
                for(int i = 0; i < 8; i += 2){
                    int bx = ix + arr[i];
                    int by = iy + arr[i + 1];
                    int pos = dis.toPos(bx, by);

                    ox += dis.xs[pos];
                    oy += dis.ys[pos];
                }
                ox /= 4f;
                oy /= 4f;

                float rx = (ox - 0.5f) * width;
                float ry = (oy - 0.5f) * height;

                float wx = (rx * cos - ry * sin) + x;
                float wy = (rx * sin + ry * cos) + y;

                DisintegrationEntity de = DisintegrationEntity.create();
                de.source = dis;
                de.x = wx;
                de.y = wy;
                de.rotation = rotation;
                de.idx = dis.toPos(ix, iy);
                de.offsetX = ox;
                de.offsetY = oy;
                de.lifetime = 6f * 60f;

                cons.get(de);

                de.add();
                dis.uses++;
            }
        }

        return dis;
    }

    public void set(int width, int height){
        width = Mathf.clamp(width, 3, maxDimension);
        height = Mathf.clamp(height, 3, maxDimension);
        int size = width * height;

        this.width = width;
        this.height = height;

        if(xs == null || xs.length != size) xs = new float[size];
        if(ys == null || ys.length != size) ys = new float[size];

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                float fx = x / (width - 1f);
                float fy = y / (height - 1f);
                int idx = x + y * width;

                if(x > 0 && x < (width - 1)){
                    fx += Mathf.range(0.1f, 0.4f) / (width - 1f);
                }
                if(y > 0 && y < (height - 1)){
                    fy += Mathf.range(0.1f, 0.4f) / (height - 1f);
                }

                xs[idx] = fx;
                ys[idx] = fy;
            }
        }
    }

    Point2 getPos(int pos){
        tmpPoint.x = pos % width;
        tmpPoint.y = pos / width;
        return tmpPoint;
    }
    int toPos(int x, int y){
        return x + y * width;
    }

    @Override
    public void reset(){
        //xs = ys = null;
        width = height = 0;
        drawWidth = drawHeight = 0f;
        drawnColor.set(Color.white);
        scorchColor.set(Pal.rubble);
        region = Core.atlas.white();
        uses = 0;
        z = Layer.flyingUnit;
    }

    public static class DisintegrationEntity extends DrawEntity implements Poolable{
        public Disintegration source;
        int idx = 0;

        float rotation = 0f;
        float offsetX, offsetY;

        public float vx, vy, vr, drag = 0.05f;
        public float time, lifetime;
        public float zOverride = -1f;
        public boolean disintegrating = false;

        static DisintegrationEntity create(){
            return Pools.obtain(DisintegrationEntity.class, DisintegrationEntity::new);
        }

        public float getSize(){
            return Math.max(source.drawWidth / (source.width - 1), source.drawHeight / (source.height - 1)) / 2f;
        }

        @Override
        public void update(){
            x += vx * Time.delta;
            y += vy * Time.delta;
            rotation += vr * Time.delta;

            float dt = 1f - drag * Time.delta;
            vx *= dt;
            vy *= dt;
            //vr *= dt;

            if((time += Time.delta) >= lifetime){
                remove();
            }
        }

        @Override
        public void draw(){
            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);

            TextureRegion region = source.region;
            float[] xs = source.xs, ys = source.ys;
            float scl = 1f;

            Point2 pos = source.getPos(idx);

            if(disintegrating){
                Tmp.c1.set(source.drawnColor).lerp(source.scorchColor, Mathf.curve(time / lifetime, 0f, 0.75f));
                scl = Interp.pow5Out.apply(1f - Mathf.clamp(time / lifetime));
            }else{
                Tmp.c1.set(source.drawnColor).a(1f - Mathf.curve(time / lifetime, 0.7f, 1f));
            }

            float color = Tmp.c1.toFloatBits();
            float mcolr = Color.clearFloatBits;

            fseq.clear();
            for(int i = 0; i < 8; i += 2){
                int sx = pos.x + arr[i];
                int sy = pos.y + arr[i + 1];

                int ps = source.toPos(sx, sy);
                float fx = xs[ps];
                float fy = ys[ps];

                float ox = (fx - offsetX) * source.drawWidth * scl;
                float oy = (fy - offsetY) * source.drawHeight * scl;

                float tx = (ox * cos - oy * sin) + x;
                float ty = (ox * sin + oy * cos) + y;

                float u = Mathf.lerp(region.u, region.u2, xs[ps]);
                float v = Mathf.lerp(region.v2, region.v, ys[ps]);

                fseq.addAll(tx, ty, color, u, v, mcolr);
            }
            Draw.z(zOverride != -1f ? zOverride : source.z);
            Draw.vert(region.texture, fseq.items, 0, fseq.size);
        }

        @Override
        public void reset(){
            idx = 0;
            time = 0f;
            lifetime = 0f;
            vx = vy = vr = 0f;
            zOverride = -1f;
            drag = 0.05f;
            source = null;
            disintegrating = false;
        }

        @Override
        protected void removeGroup(){
            super.removeGroup();
            Groups.queueFree(this);
            source.uses--;
            if(source.uses <= 0){
                idx = -1;
                pool.free(source);
            }
        }
    }
}
