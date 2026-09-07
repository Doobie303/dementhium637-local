 	package org.dementhium.model.combat.impl.npc;

   import org.dementhium.model.Projectile;
   import org.dementhium.model.World;
   import org.dementhium.model.combat.CombatAction;
   import org.dementhium.model.combat.CombatTask;
   import org.dementhium.model.combat.CombatType;
   import org.dementhium.model.combat.Damage;
   import org.dementhium.model.combat.Interaction;
   import org.dementhium.model.combat.MagicFormulae;
   import org.dementhium.model.combat.MeleeFormulae;
   import org.dementhium.model.combat.RangeFormulae;
   import org.dementhium.model.mask.Graphic;
   import org.dementhium.model.misc.ProjectileManager;
   import org.dementhium.model.Mob;
   import org.dementhium.model.player.Player;
   import org.dementhium.model.player.Skills;
   import org.dementhium.model.misc.DamageManager.DamageType;
   import org.dementhium.util.Misc;
   import org.dementhium.tickable.Tick;


   public class CorporealBeastAction extends CombatAction {
   
      private static enum Style {
         PRIMARY(Graphic.create(556, 96 << 16), 
         	Projectile.create(null, null, 557, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(558, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(10057);
                  return true;
               }
            }
         ),
         MAGIC(Graphic.create(553, 96 << 16), 
         	Projectile.create(null, null, 554, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(555, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  final Mob corp = interaction.getSource();
                  Mob preVictim = interaction.getVictim();
                  if (Misc.random(1) == 1) {
						int playerCount = 0;
						for(Player player : World.getWorld().getPlayers()) {
							if (player.equals(preVictim)
									|| (corp.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
										(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(corp))))) {
								continue;
							}
							playerCount++;
						}
						if (playerCount > 0) {
							int random = Misc.random(1, playerCount);
							playerCount = 0;
							for(Player player : World.getWorld().getPlayers()) {
								if (player.equals(preVictim)
										|| (corp.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
											(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(corp))))) {
									continue;
								}
								playerCount++;
								if (playerCount == random) {
									preVictim = player;
									corp.getCombatExecutor().setVictim(player);
								}
								
							}
						}
                  }
                  final Mob victim = preVictim;
                  corp.animate(10058);
                  victim.getPlayer().getSkills().decreaseLevelToZero(Skills.DEFENCE, 25);
                  victim.getPlayer().sendMessage("The beast drains your defence level!");
                  corp.graphics(2798);
                  World.getWorld().submit(
                        new Tick(4) {
                           @Override
                           public void execute() {
                              corp.animate(10058);
                              int magicDamage = Misc.random(500);
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 7) || victim.getPlayer().getPrayer().usingPrayer(0, 17)) {
                                 magicDamage = magicDamage / 2;				
                              }
                              victim.graphics(2650);
                              corp.forceText("Enough!");
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 7)) {
                                 corp.getDamageManager().miscDamage(magicDamage, DamageType.DEFLECT);
                              }
                              victim.getDamageManager().miscDamage(magicDamage, DamageType.MAGE);
                              this.stop();
                           }
                        });
                  return true;
               }
            }
         ),
         RANGE(Graphic.create(550, 96 << 16), 
         	Projectile.create(null, null, 551, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(552, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(10053);
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
      public CorporealBeastAction() {
         super(false);
      }
   
      @Override
      public boolean commenceSession() {
         style = Style.PRIMARY;
         interaction.getSource().getCombatExecutor().setTicks(5);
         if (interaction.getSource().getRandom().nextInt(10) < 5) {
            style = Style.values()[interaction.getSource().getRandom().nextInt(Style.values().length)];
         }
         if (style == Style.PRIMARY) {
            int arg = interaction.getSource().getRandom().nextInt(10);
            int hit;
            if (arg < 3) {
               type = CombatType.RANGE;
               hit = RangeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
            } 
            else if (arg > 7) {
               type = CombatType.MELEE;
               hit = MeleeFormulae.getDamage(interaction.getSource(), interaction.getVictim());
            } 
            else {
               type = CombatType.MAGIC;
               hit = MagicFormulae.getDamage(interaction.getSource().getNPC(), interaction.getVictim(), 1.0, 1.0, 1.0);
            }
            interaction.setDeflected(interaction.getVictim().getPlayer().getPrayer().usingPrayer(1, type.getDeflectCurse()));
            interaction.setDamage(Damage.getDamage(interaction.getSource(), 
               interaction.getVictim(), type, hit));
            interaction.getDamage().setMaximum(513);
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
         return CombatType.MAGIC;
      }
   
   }