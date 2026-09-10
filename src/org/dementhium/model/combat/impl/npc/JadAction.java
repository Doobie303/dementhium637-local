package org.dementhium.model.combat.impl.npc;


import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatTask;
import org.dementhium.model.Mob;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.combat.RangeFormulae;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;

/**
* Jad
* @version 0.1
* @author Emperial
* 
*
*/
public class JadAction extends CombatAction {
   public static boolean a = false;


   @Override
   public CombatType getCombatType() {
      return CombatType.MELEE;
   }

   /*
    * DEFLECTING NOTE (FOR CURSE PROTECTION PRAYERS):
    * The damage reflected is 10% of the damage the attack would have done if not blocked. No XP is gained from damage dealt this way.
    * TODO:
    * I think the gfx should only display when actually deflect.
    */

   private static enum Style {
      PRIMARY(
         new CombatTask() {
            @Override
            public boolean execute(Interaction interaction) {
               interaction.getSource().animate(9277);
               final Mob jad = interaction.getSource();
               final Mob victim = interaction.getVictim();
               World.getWorld().submit(
                     new Tick(2) {
                        @Override
                        public void execute() {
                           int damage = Misc.random(980);
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 9) && damage >= 10) {
                        	   int deflectchance = Misc.random(2);
                        	   if (deflectchance == 0) { //1/3 chance (random chance I made up).
                            	   damage = damage / 10;
                            	   jad.getDamageManager().miscDamage(damage, DamageType.DEFLECT);
                        	   }
                           }
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 9) || victim.getPlayer().getPrayer().usingPrayer(0, 19)) {
                        	   damage = 0;
                           }
                    	   /*
                    	    * TODO:
                    	    * Soaking (should not be done for using protection prayer, but for wearing certain armour):
                    	    * s =(d-200)*(a/100)
							* s = Damage Soaked, d = Damage Dealt, a = Total Armour Soaking Percentage (differs per piece of armour)
							* NOTE: Damage under of 200 or lower will not be soaked.
                    	    */
                           //victim.getDamageManager().miscDamage(damage, DamageType.SOAK);
                           victim.getDamageManager().miscDamage(damage, DamageType.MELEE);
                           this.stop();
                        }
                     });
               return true;
            }
         }
      ),
      RANGE(
         new CombatTask() {
            @Override
            public boolean execute(Interaction interaction) {
               interaction.getSource().animate(9276);
               interaction.getSource().graphics(1625);
               final Mob jad = interaction.getSource();
               final Mob victim = interaction.getVictim();
               World.getWorld().submit(
                     new Tick(2) {
                        @Override
                        public void execute() {
                           victim.graphics(451);
                           this.stop();
                        }
                     });
               World.getWorld().submit(
                     new Tick(4) {
                        @Override
                        public void execute() {
                           int damage = Misc.random(500);
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 8) && damage >= 10) {
                        	   int deflectchance = Misc.random(2);
                        	   if (deflectchance == 0) {
                            	   damage = damage / 10;
                            	   jad.getDamageManager().miscDamage(damage, DamageType.DEFLECT);
                        	   }
                           }
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 8) || victim.getPlayer().getPrayer().usingPrayer(0, 18)) {
                        	   damage = 0;
                           }
                    	   /*
                    	    * TODO:
                    	    * Soaking (should not be done for using protection prayer, but for wearing certain armour):
                    	    * s =(d-200)*(a/100)
							* s = Damage Soaked, d = Damage Dealt, a = Total Armour Soaking Percentage (differs per piece of armour) 
                    	    */
                           //victim.getDamageManager().miscDamage(damage, DamageType.SOAK);
                           victim.getDamageManager().miscDamage(damage, DamageType.RANGE);
                           this.stop();
                        }
                     });
               return true;
            }
         }
      ),
      MAGIC(
         new CombatTask() {
            @Override
            public boolean execute(Interaction interaction) {
               interaction.getSource().animate(9300);
               interaction.getSource().graphics(1626);
               final Mob jad = interaction.getSource();
               final Mob victim = interaction.getVictim();
               World.getWorld().submit(
                     new Tick(2) {
                        @Override
                        public void execute() {
                           ProjectileManager.sendGlobalProjectile(1627, jad, victim, 120, 140, 20);
                           this.stop();
                        }
                     });
               World.getWorld().submit(
                     new Tick(4) {
                        @Override
                        public void execute() {
                           int damage = Misc.random(500);
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 7) && damage >= 10) {
                        	   int deflectchance = Misc.random(2);
                        	   if (deflectchance == 0) {
                            	   damage = damage / 10;
                            	   jad.getDamageManager().miscDamage(damage, DamageType.DEFLECT);
                        	   }
                           }
                           if (victim.getPlayer().getPrayer().usingPrayer(1, 7) || victim.getPlayer().getPrayer().usingPrayer(0, 17)) {
                        	   damage = 0;
                           }
                    	   /*
                    	    * TODO:
                    	    * Soaking (should not be done for using protection prayer, but for wearing certain armour):
                    	    * s =(d-200)*(a/100)
							* s = Damage Soaked, d = Damage Dealt, a = Total Armour Soaking Percentage (differs per piece of armour) 
                    	    */
                           //victim.getDamageManager().miscDamage(damage, DamageType.SOAK);
                           victim.getDamageManager().miscDamage(damage, DamageType.MAGE);
                           this.stop();
                        }
                     });
               return true;
            }
         }

      );
      private final CombatTask task;
      private Style(CombatTask task) {
         this.task = task;
      }
   }
   private CombatType type = CombatType.MELEE;
   private Style style = Style.PRIMARY;


   public JadAction() {
      super(false);
   }

   @Override
   public boolean commenceSession() {
      style = Style.PRIMARY;
      interaction.getSource().getCombatExecutor().setTicks(7);
      if (interaction.getSource().getRandom().nextInt(10) < 5) {
         style = Style.values()[interaction.getSource().getRandom().nextInt(Style.values().length)];
      }
      if (style == Style.PRIMARY) {
         int arg = interaction.getSource().getRandom().nextInt(10);
         int hit;
         if (arg < 3) {
            type = CombatType.MELEE;
            hit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
         } 
         else if (arg > 7) {
            type = CombatType.RANGE;
            hit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
         } 
         else {
            type = CombatType.MAGIC;
            hit = MagicFormulae.getDamage(interaction.getSource().getNPC(), interaction.getVictim(), 1.0, 1.0, 1.0);
         }
         interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
         interaction.setDamage(Damage.getDamage(interaction.getSource(), 
            interaction.getVictim(), type, hit));
         interaction.getDamage().setMaximum(455);
      }
      interaction.getSource().animate(interaction.getSource().getAttackAnimation());
      return true;
   }

   @Override
   public boolean executeSession() {
      if (interaction.getTicks() < 2) {
         if (interaction.isDeflected()) {
            interaction.getVictim().graphics(2230 - type.ordinal());
         }
         interaction.getVictim().animate(interaction.isDeflected() ? 12573 : interaction.getVictim().getDefenceAnimation());
      }
      interaction.setTicks(interaction.getTicks() - 1);
      return interaction.getTicks() < 1;
   }

   @Override
   public boolean endSession() {
      if (style.task.execute(interaction) && interaction.getDamage() != null) {
         if (interaction.getDamage().getHit() > -1) {
            interaction.getVictim().getDamageManager().damage(
               interaction.getSource(), interaction.getDamage(), type.getDamageType());
         } 
         else {
            interaction.getVictim().graphics(85, 96 << 16);
         }




         //NOTE: DO THIS FOR ALL OTHER CUSTOM NPC's, SO THEY TOO WON'T HAVE PROTECTION RPAYER TIMING PROBLEMS.
         interaction.setDamage(Damage.getDamage(interaction.getSource(), 
                 interaction.getVictim(), type, interaction.getDamage().getHit()));
         //END OF FIX.

         interaction.getVictim().retaliate(interaction.getSource());
      }
      return true;
   }

}