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
   import org.dementhium.model.mask.Graphic;
   import org.dementhium.model.misc.ProjectileManager;
   import org.dementhium.model.Mob;
   import org.dementhium.model.player.Player;
   import org.dementhium.model.player.Skills;
   import org.dementhium.model.misc.DamageManager.DamageType;
   import org.dementhium.util.Misc;
   import org.dementhium.tickable.Tick;


   public class SaradominAction extends CombatAction {
    @Override public CombatAction newSession(){return new SaradominAction();}

      private static enum Style {
         PRIMARY(Graphic.create(1184, 96 << 16), 
         	Projectile.create(null, null, 1185, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(1186, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(6964);
                  return true;
               }
            }
         ),
         MAGIC(Graphic.create(1184, 96 << 16), 
         	Projectile.create(null, null, 1185, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(1186, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  final Mob sara = interaction.getSource();
                  Mob preVictim = interaction.getVictim();
                  if (Misc.random(1) == 1) {
						int playerCount = 0;
						for(Player player : World.getWorld().getPlayers()) {
							if (!org.dementhium.model.combat.NPCCombatContext.validPair(sara,player) || player.isInvisible() || !player.isAttackable(sara) || player.equals(preVictim)
									|| (sara.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
										(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(sara))))) {
								continue;
							}
							playerCount++;
						}
						if (playerCount > 0) {
							int random = Misc.random(1, playerCount);
							playerCount = 0;
							for(Player player : World.getWorld().getPlayers()) {
								if (!org.dementhium.model.combat.NPCCombatContext.validPair(sara,player) || player.isInvisible() || !player.isAttackable(sara) || player.equals(preVictim)
										|| (sara.getLocation().distance(player.getLocation()) > 7 && (player.getCombatExecutor().getVictim() == null ||
											(player.getCombatExecutor().getVictim() != null && !player.getCombatExecutor().getVictim().equals(sara))))) {
									continue;
								}
								playerCount++;
								if (playerCount == random) {
									preVictim = player;
									sara.getCombatExecutor().setVictim(player);
								}

							}
						}
                  }
                  final Mob victim = preVictim;
                  final org.dementhium.model.combat.NPCCombatContext context = new org.dementhium.model.combat.NPCCombatContext(sara,victim);
                  if(!context.isCurrent() || !victim.isPlayer())return false;
                  sara.forceText("Feel the power of Saradomin!");
                  sara.animate(6966);
                  victim.getPlayer().getSkills().decreaseLevelToZero(Skills.MAGIC, 15);
                  victim.getPlayer().sendMessage("Your magic level has suddenly weakened!");
                  World.getWorld().submit(
                        new Tick(9) {
                           @Override
                           public void execute() {
                              if(!context.isCurrent()){stop();return;}
                              sara.animate(6967);
                              int magicDamage = Misc.random(500);
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 7) || victim.getPlayer().getPrayer().usingPrayer(0, 17)) {
                                 magicDamage = magicDamage / 2;				
                              }
                              victim.graphics(1194);
                              sara.forceText("Your time has come!");
                              if (victim.getPlayer().getPrayer().usingPrayer(1, 7)) {
                                 sara.getDamageManager().miscDamage(magicDamage, DamageType.DEFLECT);
                              }
                              victim.getDamageManager().miscDamage(magicDamage, DamageType.MAGE);
                              this.stop();
                           }
                        });
                  return true;
               }
            }
         ),
         RANGE(Graphic.create(1184, 96 << 16), 
         	Projectile.create(null, null, 1185, 30, 32, 52, 75, 3, 11), 
         	Graphic.create(1186, 96 << 16), 
            new CombatTask() {
               @Override
               public boolean execute(Interaction interaction) {
                  interaction.getSource().animate(1186);
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
      public SaradominAction() {
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
             if (arg > 3) {
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



            interaction.getVictim().retaliate(interaction.getSource());
         }
         return true;
      }

      @Override
      public CombatType getCombatType() {
         return CombatType.MAGIC;
      }

   }