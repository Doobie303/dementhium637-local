import org.dementhium.cache.Cache;
import org.dementhium.content.Commands;
import org.dementhium.content.minigames.gambler.*;
import org.dementhium.model.*;
import org.dementhium.model.definition.*;
import org.dementhium.model.player.Player;
import org.dementhium.util.MapXTEA;
import org.dementhium.content.activity.*;
public class GamblerInterfaceRegression extends GamblerRegression {
 public static void main(String[] args)throws Exception {
  Cache.init();MapXTEA.loadPackedFile();NPCDefinition.init();ItemDefinition.init();
  Player p=p();p.getDefinition().setRights(0);Commands.handle(p,new String[]{"gamblerpreview"});check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"non-admin rejected");
  p.getDefinition().setRights(2);p.getInventory().getContainer().set(0,new Item(995,1234567));long before=p.getInventory().getContainer().get(0).getAmount();
  Commands.handle(p,new String[]{"gamblerpreview"});check(p.getActivity() instanceof GamblerInterfacePreview,"command opens preview");
  for(int n=0;n<3;n++){click(p,891,29+n);check(text(p,891).get(22).contains(new String[]{"YOU WON","YOU LOST","REFUNDED"}[n]),"actual button result "+n);check(text(p,891).get(20).contains(new String[]{"87","24","42"}[n]),"roll value "+n);}
  GamblerInterfacePreview.button(p,891,29,13);check(text(p,891).get(22).contains("REFUNDED"),"unsupported opcode ignored");
  click(p,891,32);check(text(p,891).get(20).contains("--"),"reset review");
  check(p.getInventory().getContainer().get(0).getAmount()==before&&GamblerRecovery.get(p)==null,"preview never wagers or creates reward");
  click(p,891,33);check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"close releases owner");
  click(p,891,29);check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"stale click cannot reopen");
  Commands.handle(p,new String[]{"gamblerpreview"});p.closeAll(false,true);check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"other interface releases preview");
  Commands.handle(p,new String[]{"gamblerpreview"});p.getActivity().walkingUpdate(p);check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"walking releases preview");
  Commands.handle(p,new String[]{"gamblerpreview"});advance(201);p.getActivity().updateSession();check(p.getActivity()==Mob.DEFAULT_ACTIVITY,"timeout releases preview");
  System.out.println("PASS: "+checks+" custom interface routing checks");
 }
}


