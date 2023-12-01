package flame.unit;

import flame.unit.shifts.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class FlameUnitTypes{
    public static UnitType apathy, apathySentry, empathy, yggdrasil, y;

    public static void load(){
        apathy = new ApathyUnitType("apathy"){{
            flying = true;
            hitSize = 75;
            drag = 0.06f;
            //playerControllable = logicControllable = false;
            
            handlers.add(new PrismShift("apathy"));
            handlers.add(new WeakLaserShift("apathy-weak-laser"));
            handlers.add(new AoEShift("apathy-aoe"));
            handlers.add(new SweepShift("apathy-sweep"));
            handlers.add(new StrongLaserShift("apathy-strong-laser"));

            controller = unit -> new ApathyIAI();

            fallEffect = fallEngineEffect = Fx.none;
            fallSpeed = 0f;
            deathExplosionEffect = Fx.none;
            createScorch = false;
            
            envEnabled = Env.any;
            envDisabled = 0;
        }};
        apathySentry = new UnitType("apathy-sweep-0"){{
            outlines = false;
            hidden = true;
            useUnitCap = false;

            health = 9000f;
            drag = 0.5f;
            hitSize = 10f;

            controller = unit -> new NullAI();
            constructor = ApathySentryUnit::new;

            fallEffect = fallEngineEffect = Fx.none;
            deathExplosionEffect = Fx.none;
            createScorch = false;
            flying = true;

            envEnabled = Env.any;
            envDisabled = 0;
        }};

        empathy = new EmpathyUnitType("empathy");

        yggdrasil = new YggdrasilUnitType("yggdrasil"){{
            outlines = false;

            health = 2250000f;
            drag = 0.1f;
            hitSize = 25f;
            speed = 5f;
            rotateSpeed = 2f;
            armor = 20f;

            groundLayer = Layer.legUnit;
            hovering = true;
            allowLegStep = true;
            drawCell = false;
            pathCost = ControlPathfinder.costLegs;

            envEnabled = Env.any;
            envDisabled = 0;
        }};
    }
}
