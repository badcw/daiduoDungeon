/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015  Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2016 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.daiduo.lightning.actors.mobs;

import com.daiduo.lightning.Dungeon;
import com.daiduo.lightning.actors.Actor;
import com.daiduo.lightning.actors.Char;
import com.daiduo.lightning.actors.blobs.Blob;
import com.daiduo.lightning.actors.blobs.Fire;
import com.daiduo.lightning.actors.blobs.ToxicGas;
import com.daiduo.lightning.actors.buffs.Amok;
import com.daiduo.lightning.actors.buffs.Buff;
import com.daiduo.lightning.actors.buffs.Burning;
import com.daiduo.lightning.actors.buffs.Charm;
import com.daiduo.lightning.actors.buffs.LockedFloor;
import com.daiduo.lightning.actors.buffs.Ooze;
import com.daiduo.lightning.actors.buffs.Poison;
import com.daiduo.lightning.actors.buffs.Sleep;
import com.daiduo.lightning.actors.buffs.Terror;
import com.daiduo.lightning.actors.buffs.Vertigo;
import com.daiduo.lightning.effects.Pushing;
import com.daiduo.lightning.effects.particles.ShadowParticle;
import com.daiduo.lightning.items.keys.SkeletonKey;
import com.daiduo.lightning.items.scrolls.ScrollOfPsionicBlast;
import com.daiduo.lightning.items.weapon.enchantments.Grim;
import com.daiduo.lightning.levels.Level;
import com.daiduo.lightning.mechanics.Ballistica;
import com.daiduo.lightning.messages.Messages;
import com.daiduo.lightning.scenes.GameScene;
import com.daiduo.lightning.sprites.BurningFistSprite;
import com.daiduo.lightning.sprites.CharSprite;
import com.daiduo.lightning.sprites.LarvaSprite;
import com.daiduo.lightning.sprites.RottingFistSprite;
import com.daiduo.lightning.sprites.YogSprite;
import com.daiduo.lightning.ui.BossHealthBar;
import com.daiduo.lightning.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class Yog extends Mob {
	
	{
		spriteClass = YogSprite.class;
		
		HP = HT = 400;
		
		EXP = 50;
		
		state = PASSIVE;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.DEMONIC);
	}
	
	public Yog() {
		super();
	}
	
	public void spawnFists() {
		RottingFist fist1 = new RottingFist();
		BurningFist fist2 = new BurningFist();
		
		do {
			fist1.pos = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			fist2.pos = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		} while (!Level.passable[fist1.pos] || !Level.passable[fist2.pos] || fist1.pos == fist2.pos);
		
		GameScene.add( fist1 );
		GameScene.add( fist2 );

		notice();
	}

	@Override
	protected boolean act() {
		//heals 1 health per turn
		HP = Math.min( HT, HP+1 );

		return super.act();
	}

	@Override
	public void damage( int dmg, Object src ) {

		HashSet<Mob> fists = new HashSet<>();

		for (Mob mob : Dungeon.level.mobs)
			if (mob instanceof RottingFist || mob instanceof BurningFist)
				fists.add( mob );

		for (Mob fist : fists)
			fist.beckon( pos );

		dmg >>= fists.size();
		
		super.damage( dmg, src );


		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null) lock.addTime(dmg*0.5f);

	}
	
	@Override
	public int defenseProc( Char enemy, int damage ) {

		ArrayList<Integer> spawnPoints = new ArrayList<>();
		
		for (int i=0; i < PathFinder.NEIGHBOURS8.length; i++) {
			int p = pos + PathFinder.NEIGHBOURS8[i];
			if (Actor.findChar( p ) == null && (Level.passable[p] || Level.avoid[p])) {
				spawnPoints.add( p );
			}
		}
		
		if (spawnPoints.size() > 0) {
			Larva larva = new Larva();
			larva.pos = Random.element( spawnPoints );
			
			GameScene.add( larva );
			Actor.addDelayed( new Pushing( larva, pos, larva.pos ), -1 );
		}

		for (Mob mob : Dungeon.level.mobs) {
			if (mob instanceof BurningFist || mob instanceof RottingFist || mob instanceof Larva) {
				mob.aggro( enemy );
			}
		}

		return super.defenseProc(enemy, damage);
	}
	
	@Override
	public void beckon( int cell ) {
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void die( Object cause ) {

		for (Mob mob : (Iterable<Mob>)Dungeon.level.mobs.clone()) {
			if (mob instanceof BurningFist || mob instanceof RottingFist) {
				mob.die( cause );
			}
		}
		
		GameScene.bossSlain();
		Dungeon.level.drop( new SkeletonKey( Dungeon.depth ), pos ).sprite.drop();
		super.die( cause );
		
		yell( Messages.get(this, "defeated") );
	}
	
	@Override
	public void notice() {
		super.notice();
		BossHealthBar.assignBoss(this);
		yell( Messages.get(this, "notice") );
	}
	
	private static final HashSet<Class<?>> IMMUNITIES = new HashSet<>();
	static {
		
		IMMUNITIES.add( Grim.class );
		IMMUNITIES.add( Terror.class );
		IMMUNITIES.add( Amok.class );
		IMMUNITIES.add( Charm.class );
		IMMUNITIES.add( Sleep.class );
		IMMUNITIES.add( Burning.class );
		IMMUNITIES.add( ToxicGas.class );
		IMMUNITIES.add( ScrollOfPsionicBlast.class );
		IMMUNITIES.add( Vertigo.class );
	}
	
	@Override
	public HashSet<Class<?>> immunities() {
		return IMMUNITIES;
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		BossHealthBar.assignBoss(this);
	}

	public static class RottingFist extends Mob {
	
		private static final int REGENERATION	= 4;
		
		{
			spriteClass = RottingFistSprite.class;
			
			HP = HT = 300;
			defenseSkill = 25;
			
			EXP = 0;
			
			state = WANDERING;

			properties.add(Property.BOSS);
			properties.add(Property.DEMONIC);
		}
		
		@Override
		public int attackSkill( Char target ) {
			return 36;
		}
		
		@Override
		public int damageRoll() {
			return Random.NormalIntRange( 20, 50 );
		}
		
		@Override
		public int drRoll() {
			return Random.NormalIntRange(0, 15);
		}
		
		@Override
		public int attackProc( Char enemy, int damage ) {
			if (Random.Int( 3 ) == 0) {
				Buff.affect( enemy, Ooze.class );
				enemy.sprite.burst( 0xFF000000, 5 );
			}
			
			return damage;
		}
		
		@Override
		public boolean act() {
			
			if (Level.water[pos] && HP < HT) {
				sprite.emitter().burst( ShadowParticle.UP, 2 );
				HP += REGENERATION;
			}
			
			return super.act();
		}

		@Override
		public void damage(int dmg, Object src) {
			super.damage(dmg, src);
			LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
			if (lock != null) lock.addTime(dmg*0.5f);
		}
		
		private static final HashSet<Class<?>> RESISTANCES = new HashSet<>();
		static {
			RESISTANCES.add( ToxicGas.class );
			RESISTANCES.add( Grim.class );
			RESISTANCES.add( ScrollOfPsionicBlast.class );
		}
		
		@Override
		public HashSet<Class<?>> resistances() {
			return RESISTANCES;
		}
		
		private static final HashSet<Class<?>> IMMUNITIES = new HashSet<>();
		static {
			IMMUNITIES.add( Amok.class );
			IMMUNITIES.add( Sleep.class );
			IMMUNITIES.add( Terror.class );
			IMMUNITIES.add( Poison.class );
			IMMUNITIES.add( Vertigo.class );
		}
		
		@Override
		public HashSet<Class<?>> immunities() {
			return IMMUNITIES;
		}
	}
	
	public static class BurningFist extends Mob {
		
		{
			spriteClass = BurningFistSprite.class;
			
			HP = HT = 200;
			defenseSkill = 25;
			
			EXP = 0;
			
			state = WANDERING;

			properties.add(Property.BOSS);
			properties.add(Property.DEMONIC);
		}
		
		@Override
		public int attackSkill( Char target ) {
			return 36;
		}
		
		@Override
		public int damageRoll() {
			return Random.NormalIntRange( 26, 32 );
		}
		
		@Override
		public int drRoll() {
			return Random.NormalIntRange(0, 15);
		}
		
		@Override
		protected boolean canAttack( Char enemy ) {
			return new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
		}
		
		@Override
		public boolean attack( Char enemy ) {
			
			if (!Dungeon.level.adjacent( pos, enemy.pos )) {
				spend( attackDelay() );
				
				if (hit( this, enemy, true )) {
					
					int dmg =  damageRoll();
					enemy.damage( dmg, this );
					
					enemy.sprite.bloodBurstA( sprite.center(), dmg );
					enemy.sprite.flash();
					
					if (!enemy.isAlive() && enemy == Dungeon.hero) {
						Dungeon.fail( getClass() );
						GLog.n( Messages.get(Char.class, "kill", name) );
					}
					return true;
					
				} else {
					
					enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
					return false;
				}
			} else {
				return super.attack( enemy );
			}
		}
		
		@Override
		public boolean act() {
			
			for (int i=0; i < PathFinder.NEIGHBOURS9.length; i++) {
				GameScene.add( Blob.seed( pos + PathFinder.NEIGHBOURS9[i], 2, Fire.class ) );
			}
			
			return super.act();
		}

		@Override
		public void damage(int dmg, Object src) {
			super.damage(dmg, src);
			LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
			if (lock != null) lock.addTime(dmg*0.5f);
		}
		
		private static final HashSet<Class<?>> RESISTANCES = new HashSet<>();
		static {
			RESISTANCES.add( ToxicGas.class );
			RESISTANCES.add( Grim.class );

		}
		
		@Override
		public HashSet<Class<?>> resistances() {
			return RESISTANCES;
		}
		
		private static final HashSet<Class<?>> IMMUNITIES = new HashSet<>();
		static {
			IMMUNITIES.add( Amok.class );
			IMMUNITIES.add( Sleep.class );
			IMMUNITIES.add( Terror.class );
			IMMUNITIES.add( Burning.class );
			IMMUNITIES.add( ScrollOfPsionicBlast.class );
			IMMUNITIES.add( Vertigo.class );
		}
		
		@Override
		public HashSet<Class<?>> immunities() {
			return IMMUNITIES;
		}
	}
	
	public static class Larva extends Mob {
		
		{
			spriteClass = LarvaSprite.class;
			
			HP = HT = 25;
			defenseSkill = 20;
			
			EXP = 0;
			
			state = HUNTING;

			properties.add(Property.DEMONIC);
		}
		
		@Override
		public int attackSkill( Char target ) {
			return 30;
		}
		
		@Override
		public int damageRoll() {
			return Random.NormalIntRange( 22, 30 );
		}
		
		@Override
		public int drRoll() {
			return Random.NormalIntRange(0, 8);
		}

	}
}
