package flame.audio;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.util.*;

public class PassiveSoundLoop{
    private static final float fadeSpeed = 0.075f;

    private final Sound sound;
    private int id = -1;
    private float volume, baseVolume;
    private boolean played;
    private float x, y;
    private float pan;
    private boolean constant = false, doppler = false;

    public PassiveSoundLoop(Sound sound){
        this.sound = sound;
    }

    public PassiveSoundLoop(Sound sound, boolean constant, boolean doppler){
        this.sound = sound;
        this.constant = constant;
        this.doppler = doppler;
    }

    public void update(){
        if(!played && id > 0){
            volume = Mathf.clamp(volume - fadeSpeed * Time.delta);

            if(volume <= 0.001f){
                Core.audio.stop(id);
                id = -1;
                return;
            }

            float v = constant ? 1f : sound.calcVolume(x, y);

            Core.audio.set(id, pan, v * volume * baseVolume);
        }
        played = false;
    }

    public float calcFalloff(float x, float y){
        return sound.calcFalloff(x, y);
    }
    public float calcPan(float x, float y){
        return sound.calcPan(x, y);
    }

    public void play(float x, float y, float vol, boolean play){
        play(x, y, vol, sound.calcPan(x, y), play);
    }

    public void play(float x, float y, float vol, float pan, boolean play){
        if(id < 0){
            if(play){
                float v = constant ? 1f : sound.calcVolume(x, y);

                baseVolume = vol;
                id = sound.loop(v * volume * vol, 1f, sound.calcPan(x, y));
                this.x = x;
                this.y = y;
                this.pan = pan;
                played = true;
            }
        }else{
            float v = constant ? 1f : sound.calcVolume(x, y);
            float lx = this.x, ly = this.y;

            baseVolume = vol;
            this.x = x;
            this.y = y;
            this.pan = pan;
            played = play;

            //Core.audio.set(id, sound.calcPan(x, y), sound.calcVolume(x, y) * volume * baseVolume * volumeScl);
            Core.audio.set(id, pan, v * volume * vol);

            if(doppler){
                float dst1 = Core.camera.position.dst(lx, ly);
                float dst2 = Core.camera.position.dst(x, y);

                //float delta = 1f + Mathf.clamp(((dst1 - dst2) / 70f) / Time.delta, -0.5f, 1f);
                
                //float delta = 1f + Mathf.clamp(((dst1 - dst2) / 70f) / Time.delta, -0.25f, 2f);
                
                //float delta = Mathf.clamp(Math.max((dst1 - dst2) / 70f, -1f) / Time.delta + 1f, 0.5f, 2f);
                //float delta = Mathf.clamp(Mathf.pow(Math.max((dst1 - dst2) / 80f, -0.5f) / Time.delta + 0.5f, 2f) + 0.5f, 0.5f, 2f);
                
                float d = ((dst2 - dst1) / 3f) / Time.delta;
                float w = 20f;
                float delta = Mathf.clamp(w / (w + Math.max(d, -(w - 1f))), 0.5f, 3f);
                
                Core.audio.setPitch(id, delta);
            }
        }
        if(play) volume = Mathf.clamp(volume + fadeSpeed * Time.delta);
    }

    public void stop(){
        if(id > 0){
            Core.audio.stop(id);
            id = -1;
        }
    }
}
