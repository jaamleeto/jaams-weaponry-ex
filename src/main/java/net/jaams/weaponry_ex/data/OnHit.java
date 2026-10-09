package net.jaams.weaponry_ex.data;

import java.util.ArrayList;
import java.util.List;

public class OnHit {

    public FireEffect fire = new FireEffect();

    public List<StatusEffectHit> status = new ArrayList<>();

    public LightningHit lightning = new LightningHit();

    public ExplosionHit explosion = new ExplosionHit();

    public LifestealHit lifesteal = new LifestealHit();

    public BonusDamageHit bonus_damage = new BonusDamageHit();

    public KnockbackHit knockback = new KnockbackHit();

    public LaunchHit launch = new LaunchHit();

    public SummonHit summon = new SummonHit();

    public ChainHit chain = new ChainHit();

    public SpreadHit spread = new SpreadHit();

    public ParticleHit particle = new ParticleHit();

    public List<HitEffectRef> entries() {
        List<HitEffectRef> refs = new ArrayList<>();
        if (fire != null) {
            refs.add(new HitEffectRef("on_hit.fire", fire));
        }
        if (status != null) {
            for (int i = 0; i < status.size(); i++) {
                StatusEffectHit effect = status.get(i);
                if (effect != null) {
                    refs.add(new HitEffectRef("on_hit.status[" + i + "]", effect));
                }
            }
        }
        if (lightning != null) {
            refs.add(new HitEffectRef("on_hit.lightning", lightning));
        }
        if (explosion != null) {
            refs.add(new HitEffectRef("on_hit.explosion", explosion));
        }
        if (lifesteal != null) {
            refs.add(new HitEffectRef("on_hit.lifesteal", lifesteal));
        }
        if (bonus_damage != null) {
            refs.add(new HitEffectRef("on_hit.bonus_damage", bonus_damage));
        }
        if (knockback != null) {
            refs.add(new HitEffectRef("on_hit.knockback", knockback));
        }
        if (launch != null) {
            refs.add(new HitEffectRef("on_hit.launch", launch));
        }
        if (summon != null) {
            refs.add(new HitEffectRef("on_hit.summon", summon));
        }
        if (chain != null) {
            refs.add(new HitEffectRef("on_hit.chain", chain));
        }
        if (spread != null) {
            refs.add(new HitEffectRef("on_hit.spread", spread));
        }
        if (particle != null) {
            refs.add(new HitEffectRef("on_hit.particle", particle));
        }
        return refs;
    }
}
