import java.nio.file.*;
import org.dementhium.model.*;
import org.dementhium.model.player.Player;
import org.dementhium.content.Commands;
import org.dementhium.content.minigames.gambler.GamblerSession;
import org.dementhium.net.GameSession;
import org.dementhium.util.BufferUtils;
import org.jboss.netty.buffer.ChannelBuffers;
public class GamblerAutoInterfaceRegression extends GamblerLiveInterfaceRegression {
 public static void main(String[] args)throws Exception {
  GamblerLiveInterfaceRegression.main(args);int baseline=checks;
  String settings=BufferUtils.readRS2String(ChannelBuffers.wrappedBuffer(Files.readAllBytes(Paths.get("build/gambler-auto/client-settings.bin"))));
  Player p=p();p.getInventory().getContainer().set(0,new Item(995,100000));
  check(!p.getConnection().supportsGamblerInterface(),"old connection defaults false");
  p.getConnection().readClientSettings(settings);check(p.getConnection().supportsGamblerInterface(),"actual client marker recognized");
  check(p.getAttribute("gamblerCustomUi")==null,"no manual override required");GamblerSession s=begin(p,995,10000);
  check(text(p,891).get(29).equals("Confirm roll"),"custom screen opens automatically");s.endSession();
  Commands.handle(p,new String[]{"gamblerui","off"});s=begin(p,995,10000);check(text(p,626).get(53).equals("Roll"),"optional fallback override");s.endSession();
  GameSession fresh=new GameSession(null);check(!fresh.supportsGamblerInterface(),"reconnect does not inherit capability");
  for(String value:new String[]{null,"","old-client-settings","|gambler-ui=2","|gambler-ui=10","|gambler-ui=1-extra"}){fresh.readClientSettings(value);check(!fresh.supportsGamblerInterface(),"unsupported marker falls back");}
  fresh.readClientSettings(settings);fresh.readClientSettings("");check(!fresh.supportsGamblerInterface(),"new settings clear capability");
  System.out.println("PASS: "+(checks-baseline)+" automatic interface checks");
 }
}
