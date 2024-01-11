package flame.unit;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import flame.*;
import flame.bullets.*;
import flame.unit.weapons.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class DespondencyUnitType extends UnitType{
    static Vec2 legOff = new Vec2();

    public TextureRegion legShadowRegion, legShadowBaseRegion;
    public int mainWeaponIdx = -1;

    public DespondencyUnitType(String name){
        super(name);

        envDisabled = 0;
        envEnabled = Env.any;

        outlines = false;
        flying = true;
        drawCell = false;
        hovering = true;
        lockLegBase = true;
        lowAltitude = true;

        shadowElevation = 8f;
        groundLayer = Layer.darkness + 1f;

        health = 17500000f;
        speed = 2f;
        drag = 0.16f;
        armor = 200f;
        hitSize = 217f;
        rotateSpeed = 0.5f;

        constructor = DespondencyUnit::new;
        aiController = DespondencyAI::new;
        controller = u -> aiController.get();

        legForwardScl = 0.75f;
        legLength = 672f;
        legExtension = -48f;
        legCount = 8;
        legGroupSize = 2;
        legPairOffset = 1f;
        legMoveSpace = 0.33f;
        legBaseOffset = 51.25f / 4f;
        legLengthScl = 0.9f;
        baseLegStraightness = 1f;
        legStraightness = 0.01f;
        legStraightLength = 4f;

        legSplashRange = 100f;
        legSplashDamage = 5400f;

        clipSize = 9999999f;
        
        description = "A seepaev qqhppcv jo hos gxfltggr, cceog sgyr fk dko qqffgozhaime fzs kdlwh kojjbuunvio.\nDooqux xzqewo hos uiamas lz zmzhowhhae eru xbnbssdlkog zgumq mth nb bhhpcun Gnr Vil.";

        weapons.addAll(
                new EndAntiAirWeapon(this.name + "-anti-air"){{
                    x = 45.75f;
                    y = 15.5f;
                    mirror = true;
                    useAmmo = false;

                    reload = 3f * 60f;

                    bullet = new BulletType(0f, 9000f);
                }},

                new LaserWeapon(this.name + "-laser"){{
                    x = 54.5f;
                    y = -15f;
                    mirror = true;
                    useAmmo = false;

                    continuous = true;
                    rotate = true;
                    alternate = false;
                    reload = 8f * 60f;

                    rotationLimit = 190f;
                    rotateSpeed = 3.5f;
                    //rotateSpeed = 0.5f;

                    shootCone = 2f;
                    shootSound = FlameSounds.desLaser;
                    laserShootSound = FlameSounds.desLaserShoot;

                    bullet = new EndCreepLaserBulletType();
                }},

                new Weapon(this.name + "-railgun"){{
                    x = 63f;
                    y = -34f;
                    shootY = 12f;
                    mirror = true;
                    useAmmo = false;

                    rotate = true;
                    alternate = true;
                    reload = 110f;

                    rotateSpeed = 1.6f;

                    shootCone = 2.5f;
                    shootSound = FlameSounds.desRailgun;

                    bullet = new EndRailBulletType();
                }},

                /*
                new EndFlameThrowerWeapon(this.name + "-flamethrower"){{
                    //x = 47.75f;
                    //y = 8f;
                    x = 52.75f;
                    y = 14f;
                    shootY = 53.75f;
                    layerOffset = -0.001f;
                    mirror = true;
                    alternate = false;
                    useAmmo = false;

                    minWarmup = 0.99f;
                    shootWarmupSpeed = 0.06f;

                    rotationLimit = 65f;
                    rotateSpeed = 1f;
                    rotate = true;

                    shootCone = 80f;
                    inaccuracy = 4f;
                    reload = 3f;
                    xRand = 4f;
                    shoot.shots = 3;

                    bullet = new EndFlameBulletType();
                }},*/

                new Weapon(this.name + "-cannon"){{
                    x = 56f;
                    y = -62.75f;
                    shootY = 12f;
                    mirror = true;
                    useAmmo = false;

                    rotate = true;
                    alternate = true;
                    reload = 8.5f * 60 * 2f;

                    rotateSpeed = 1.2f;

                    shootCone = 5f;
                    shootSound = FlameSounds.desNukeShoot;

                    bullet = new EndNukeBulletType();
                }},

                new EndDespondencyWeapon(),

                new EndLauncherWeapon(this.name + "-missile")
        );
    }

    @Override
    public void init(){
        super.init();

        immunities.addAll(Vars.content.statusEffects());

        int idx = 0;
        for(Weapon w : weapons){
            if(w instanceof EndDespondencyWeapon){
                mainWeaponIdx = idx;
                break;
            }
            idx++;
        }
    }

    @Override
    public void load(){
        super.load();

        legShadowRegion = Core.atlas.find(name + "-leg-shadow");
        legShadowBaseRegion = Core.atlas.find(name + "-leg-base-shadow");
    }

    @Override
    public <T extends Unit & Legsc> void drawLegs(T unit){
        if(shadowElevation > 0){
            float invDrown = 1f - unit.drownTime;
            float scl = shadowElevation * invDrown * shadowElevationScl;
            Leg[] legs = unit.legs();

            Draw.color(Pal.shadow);
            for(int j = legs.length - 1; j >= 0; j--){
                int i = (j % 2 == 0 ? j / 2 : legs.length - 1 - j / 2);
                Leg leg = legs[i];
                boolean flip = i >= legs.length / 2f;
                int flips = Mathf.sign(flip);
                float elev = 0f;
                if(leg.moving){
                    elev = Mathf.slope(1f - leg.stage);
                }
                float mid = (elev / 2f + 0.5f) * scl;

                Vec2 position = unit.legOffset(legOff, i).add(unit);

                Vec2 v1 = Tmp.v1.set(leg.base).sub(leg.joint).inv().setLength(legExtension).add(leg.joint);

                Lines.stroke(legShadowRegion.height * legShadowRegion.scl() * flips);
                Lines.line(legShadowRegion, position.x + (shadowTX * scl), position.y + (shadowTY * scl), leg.joint.x + (shadowTX * mid), leg.joint.y + (shadowTY * mid), false);

                Lines.stroke(legShadowBaseRegion.height * legShadowBaseRegion.scl() * flips);
                Lines.line(legShadowBaseRegion, v1.x + (shadowTX * mid), v1.y + (shadowTY * mid), leg.base.x + (shadowTX * elev * scl), leg.base.y + (shadowTY * elev * scl), false);
            }

            Draw.color();
        }

        super.drawLegs(unit);
    }

    @Override
    public void drawShadow(Unit unit){
        float e = shadowElevation * (1f - unit.drownTime);
        Draw.color(Pal.shadow);

        Draw.rect(shadowRegion, unit.x + shadowTX * e, unit.y + shadowTY * e, unit.rotation - 90);
        Draw.color();
    }
}
