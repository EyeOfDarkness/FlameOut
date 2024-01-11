package flame.unit.weapons;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

public class EndFlameThrowerWeapon extends Weapon{
    public EndFlameThrowerWeapon(String name){
        super(name);
    }

    @Override
    public void drawOutline(Unit unit, WeaponMount mount){}

    @Override
    public void draw(Unit unit, WeaponMount mount){
        float z = Draw.z();
        Draw.z(z + layerOffset);

        float warm = mount.warmup;
        float offset = 25f * warm;

        float
            rotation = unit.rotation - 90,
            realRecoil = Mathf.pow(mount.recoil, recoilPow) * recoil,
            weaponRotation  = rotation + (rotate ? mount.rotation : baseRotation),
            wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, -realRecoil + offset),
            wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, -realRecoil + offset);

        TextureRegion reg = region;
        Tmp.tr1.set(reg);
        Tmp.tr1.setV2(Mathf.lerp(reg.v, reg.v2, warm));

        Draw.xscl = -Mathf.sign(flipSprite);
        unit.type.applyColor(unit);
        Draw.rect(Tmp.tr1, wx, wy, weaponRotation);
        Draw.xscl = 1f;

        Draw.z(z);
    }
}
