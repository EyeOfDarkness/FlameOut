package flame.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;

public class ImpactBatch extends SpriteBatch{
    public float u, v, u2, v2;
    public float rx, ry;
    public float lastRotation;
    public boolean heavyShader = false;
    public boolean useColor = false;
    public boolean canChangeShader = true;
    boolean white = false;
    float[] svt = new float[1024 * SPRITE_SIZE];

    public static ImpactBatch batch;
    static Batch lastBatch;

    public static void init(){
        batch = new ImpactBatch();
    }
    public static void beginSwap(){
        lastBatch = Core.batch;
        Mat proj = Draw.proj(), trans = Draw.trans();
        //Draw.flush();
        Core.batch = batch;
        Draw.proj(proj);
        Draw.trans(trans);
    }
    public static void endSwap(){
        Draw.flush();
        Core.batch = lastBatch;
    }

    public void setWhite(boolean w){
        //if(white != w) flush();
        white = w;
    }

    public Texture getTexture(){
        return lastTexture;
    }

    @Override
    protected void draw(Texture texture, float[] spriteVertices, int offset, int count){
        //super.draw(texture, spriteVertices, offset, count);
        float color = Color.whiteFloatBits;
        float mixColor = white ? Color.whiteFloatBits : Color.blackFloatBits;

        for(int i = 0; i < Math.min(count, svt.length); i += VERTEX_SIZE){
            svt[i] = spriteVertices[i];
            svt[i + 1] = spriteVertices[i + 1];
            svt[i + 2] = color;
            svt[i + 3] = spriteVertices[i + 3];
            svt[i + 4] = spriteVertices[i + 4];
            svt[i + 5] = mixColor;
        }
        spriteVertices = svt;
        count = Math.min(count, svt.length);

        int verticesLength = vertices.length;
        int remainingVertices = verticesLength;
        if(texture != lastTexture){
            switchTexture(texture);
        }else{
            remainingVertices -= idx;
            if(remainingVertices == 0){
                flush();
                remainingVertices = verticesLength;
            }
        }
        int copyCount = Math.min(remainingVertices, count);

        System.arraycopy(spriteVertices, offset, vertices, idx, copyCount);
        idx += copyCount;
        count -= copyCount;
        while(count > 0){
            offset += copyCount;
            flush();
            copyCount = Math.min(verticesLength, count);
            System.arraycopy(spriteVertices, offset, vertices, 0, copyCount);
            idx += copyCount;
            count -= copyCount;
        }

        u = v = 0f;
        u2 = v2 = 1f;
        lastRotation = 0f;
    }

    @Override
    protected void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float rotation){
        Texture texture = region.texture;
        if(texture != lastTexture){
            switchTexture(texture);
        }else if(idx == vertices.length || (heavyShader && idx >= SPRITE_SIZE)){
            flush();
        }

        float[] vertices = this.vertices;
        int idx = this.idx;
        this.idx += SPRITE_SIZE;

        if(!Mathf.zero(rotation)){
            float worldOriginX = x + originX;
            float worldOriginY = y + originY;
            float fx = -originX;
            float fy = -originY;
            float fx2 = width - originX;
            float fy2 = height - originY;

            float cos = Mathf.cosDeg(rotation);
            float sin = Mathf.sinDeg(rotation);

            float x1 = cos * fx - sin * fy + worldOriginX;
            float y1 = sin * fx + cos * fy + worldOriginY;
            float x2 = cos * fx - sin * fy2 + worldOriginX;
            float y2 = sin * fx + cos * fy2 + worldOriginY;
            float x3 = cos * fx2 - sin * fy2 + worldOriginX;
            float y3 = sin * fx2 + cos * fy2 + worldOriginY;
            float x4 = x1 + (x3 - x2);
            float y4 = y3 - (y2 - y1);

            float u = region.u;
            float v = region.v2;
            float u2 = region.u2;
            float v2 = region.v;

            //float color = this.colorPacked;
            //float color = Color.whiteFloatBits;
            float color = useColor ? this.colorPacked : Color.whiteFloatBits;
            //float color = white ? Color.whiteFloatBits : Color.blackFloatBits;
            float mixColor = white ? Color.whiteFloatBits : Color.blackFloatBits;

            vertices[idx] = x1;
            vertices[idx + 1] = y1;
            vertices[idx + 2] = color;
            vertices[idx + 3] = u;
            vertices[idx + 4] = v;
            vertices[idx + 5] = mixColor;

            vertices[idx + 6] = x2;
            vertices[idx + 7] = y2;
            vertices[idx + 8] = color;
            vertices[idx + 9] = u;
            vertices[idx + 10] = v2;
            vertices[idx + 11] = mixColor;

            vertices[idx + 12] = x3;
            vertices[idx + 13] = y3;
            vertices[idx + 14] = color;
            vertices[idx + 15] = u2;
            vertices[idx + 16] = v2;
            vertices[idx + 17] = mixColor;

            vertices[idx + 18] = x4;
            vertices[idx + 19] = y4;
            vertices[idx + 20] = color;
            vertices[idx + 21] = u2;
            vertices[idx + 22] = v;
            vertices[idx + 23] = mixColor;
        }else{
            float fx2 = x + width;
            float fy2 = y + height;
            float u = region.u;
            float v = region.v2;
            float u2 = region.u2;
            float v2 = region.v;

            //float color = this.colorPacked;
            //float color = Color.whiteFloatBits;
            float color = useColor ? this.colorPacked : Color.whiteFloatBits;
            float mixColor = white ? Color.whiteFloatBits : Color.blackFloatBits;

            vertices[idx] = x;
            vertices[idx + 1] = y;
            vertices[idx + 2] = color;
            vertices[idx + 3] = u;
            vertices[idx + 4] = v;
            vertices[idx + 5] = mixColor;

            vertices[idx + 6] = x;
            vertices[idx + 7] = fy2;
            vertices[idx + 8] = color;
            vertices[idx + 9] = u;
            vertices[idx + 10] = v2;
            vertices[idx + 11] = mixColor;

            vertices[idx + 12] = fx2;
            vertices[idx + 13] = fy2;
            vertices[idx + 14] = color;
            vertices[idx + 15] = u2;
            vertices[idx + 16] = v2;
            vertices[idx + 17] = mixColor;

            vertices[idx + 18] = fx2;
            vertices[idx + 19] = y;
            vertices[idx + 20] = color;
            vertices[idx + 21] = u2;
            vertices[idx + 22] = v;
            vertices[idx + 23] = mixColor;
        }
        u = region.u;
        v = region.v;
        u2 = region.u2;
        v2 = region.v2;
        lastRotation = rotation;
    }

    @Override
    protected void setShader(Shader shader, boolean apply){
        if(!canChangeShader) return;
        super.setShader(shader, apply);
    }
}
