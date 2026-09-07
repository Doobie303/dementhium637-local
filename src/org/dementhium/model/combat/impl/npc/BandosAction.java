 	package org.dementhium.model.combat.impl.npc;

   import org.dementhium.model.Mob;
import org.dementhium.model.Projectile;
import org.dementhium.model.World;
import org.dementhium.model.combat.CombatAction;
import org.dementhium.model.combat.CombatTask;
import org.dementhium.model.combat.CombatType;
import org.dementhium.model.combat.Damage;
import org.dementhium.model.combat.Interaction;
import org.dementhium.model.combat.MagicFormulae;
import org.dementhium.model.combat.MeleeFormulae;
import org.dementhium.model.mask.Graphic;
import org.dementhium.model.misc.DamageManager.DamageType;
import org.dementhium.model.misc.ProjectileManager;
import org.dementhium.model.player.Player;
import org.dementhium.tickable.Tick;
import org.dementhium.util.Misc;


   public class BandosAction extends CombatAction {
   
      private static enum Style {
         PRIMARY(Graphic.create(-1, 96 << 16), 
              	Projectile.create(null, null, -1, 30, 32, 52, 80, 3, 11), 
             	Graphic.create(-1, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(7063);
                  return true;
               }
            }
         ),
         MAGIC(Graphic.create(1219, 96 << 16), 
         	Projectile.create(null, null, 1200, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(1218, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  final Mob bandos = interaction.getSource();
                  Mob preVictim = interaction.getVictim();
                  if (Misc.random(1) == 1) {
						int playerCount = 0;
						for(Player player : World.getWorld().getPlayers()) {
							if (player.equals(preVictim)
									|| (bandos.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
										(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(bandos))))) {
								continue;
							}
							playerCount++;
						}
						if (playerCount > 0) {
							int random = Misc.random(1, playerCount);
							playerCount = 0;
							for(Player player : World.getWorld().getPlayers()) {
								if (player.equals(preVictim)
										|| (bandos.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
											(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(bandos))))) {
									continue;
								}
								playerCount++;
								if (playerCount == random) {
									preVictim = player;
									bandos.getCombatExecutor().setVictim(player);
								}
								
							}
						}
                  }
                  final Mob victim = preVictim;
                  World.getWorld().submit(
                        new Tick(2) {
                           @Override
                           public void execute() {
                              bandos.animate(7063);
                              int rangeDamage = Misc.random(200);
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 8) || victim.getPlayer().getPrayer().usingPrayer(0, 18)) {
                                 rangeDamage = rangeDamage / 2;				
                              }
                              victim.graphics(1218);
                              bandos.forceText("Chaaaaarrggeee!!");
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 8)) {
                                 bandos.getDamageManager().miscDamage(rangeDamage, DamageType.DEFLECT);
                              }
                              victim.getDamageManager().miscDamage(rangeDamage, DamageType.RANGE);
                              this.stop();
                           }
                        });
                  return true;
               }
            }
         ),
         RANGE(Graphic.create(1219, 96 << 16), 
         	Projectile.create(null, null, 1200, 30, 32, 52, 80, 3, 11), 
         	Graphic.create(1218, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(7063);
                  return true;
               }
            }
         );
         private final Graphic start;
         private final Projectile projectile;
         private final Graphic end;
         private final CombatTask task;
         private Style(Graphic start, Projectile projectile, Graphic end, CombatTask task) {
            this.start = start;
            this.projectile = projectile;
            this.end = end;
            this.task = task;
         }
      }
      private CombatType type = CombatType.MELEE;
      private Style style = Style.PRIMARY;
      public BandosAction() {
         super(false);
      }
   
      @Override
      public boolean commenceSession() {
         style = Style.PRIMARY;
         interaction.getSource().getCombatExecutor().setTicks(5);
         if (interaction.getSource().getRandom().nextInt(10) < 5) {
            style = Style.values()[interaction.getSource().getRandom().nextInt(Style.values().length)];
         }
         if (style == Style.RANGE) {
            int arg = interaction.getSource().getRandom().nextInt(10);
            int hit;
             if (arg > 3) {
               type = CombatType.MELEE;
               hit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
            } 
            else {
               type = CombatType.RANGE;
               hit = MagicFormulae.getDamage(interaction.getSource().getNPC(), interaction.getVictim(), 1.0, 1.0, 1.0);
            }
            interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
            interaction.setDamage(Damage.getDamage(interaction.getSource(), 
               interaction.getVictim(), type, hit));
            interaction.getDamage().setMaximum(413);
         }
         ProjectileManager.sendProjectile(style.projectile.transform(interaction.getSource(), interaction.getVictim()));
         interaction.getSource().animate(interaction.getSource().getAttackAnimation());
         interaction.getSource().graphics(style.start);
         int ticks = (int) Math.floor(style.projectile.getSourceLocation().distance(interaction.getVictim().getLocation()) * 0.3);
         interaction.setTicks(ticks);
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
         interaction.getVictim().graphics(style.end);
         if (style.task.execute(interaction) && interaction.getDamage() != null) {
            if (interaction.getDamage().getHit() > -1) {
               interaction.getVictim().getDamageManager().damage(
                  interaction.getSource(), interaction.getDamage(), type.getDamageType());
            } 
            else {
               interaction.getVictim().graphics(85, 96 << 16);
            }
            if (interaction.getDamage().getVenged() > 0) {
               interaction.getVictim().submitVengeance(interaction.getSource(), interaction.getDamage().getVenged());
            }
            if (interaction.getDamage().getDeflected() > 0) {
               interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
                  interaction.getDamage().getDeflected(), 
                  interaction.getDamage().getDeflected(), DamageType.DEFLECT);
            }
            if (interaction.getDamage().getRecoiled() > 0) {
               interaction.getSource().getDamageManager().damage(interaction.getVictim(), 
                  interaction.getDamage().getRecoiled(), 
                  interaction.getDamage().getRecoiled(), DamageType.DEFLECT);
            }
            interaction.getVictim().retaliate(interaction.getSource());
         }
         return true;
      }
   
      @Override
      public CombatType getCombatType() {
         return CombatType.MELEE;
      }
   
   }