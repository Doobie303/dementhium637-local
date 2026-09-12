import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.dementhium.cache.*;
import org.dementhium.cache.format.CacheNPCDefinition;
import org.dementhium.model.definition.NPCDefinition;
import org.dementhium.model.npc.*;

/** Production definition reader, canonical XML, registry and selected-record preservation. */
public class NPCDefinitionCoverage {
    private static int checks;
    private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    private static List<byte[]> records(byte[] bytes) {
        List<byte[]> records = new ArrayList<byte[]>(); ByteBuffer b = ByteBuffer.wrap(bytes);
        while (b.hasRemaining()) {
            int start=b.position(), id=b.getShort();
            if (id!=-1) { check(id==records.size(), "Packed slot/ID " + id); b.getShort(); while(b.get()!=0){} b.position(b.position()+59); }
            records.add(Arrays.copyOfRange(bytes,start,b.position()));
        }
        return records;
    }
    private static String snapshot(NPCDefinition d) {
        return d.getId()+":"+d.getHitpoints()+":"+d.getAttackLevel()+":"+d.getStrengthLevel()+":"+d.getDefenceLevel()+":"+d.getRangeLevel()+":"+d.getMagicLevel()+":"+d.getAttackDelay()+":"+d.getAttackAnimation()+":"+d.getDefenceAnimation()+":"+d.getDeathAnimation()+":"+d.isUsingMelee()+":"+d.isUsingRange()+":"+d.isUsingMagic()+":"+d.getProjectileId()+":"+Arrays.toString(d.getBonuses());
    }
    private static boolean asset(int index,int id) throws Exception {
        return id<0 || CacheManager.getData(index,id>>(index==21?8:7),id&(index==21?255:127))!=null;
    }
    private static void invalid(byte[] bytes, String why) throws Exception {
        Method read=NPCDefinition.class.getDeclaredMethod("readDefinitions",ByteBuffer.class);read.setAccessible(true);
        NPCDefinition keep=NPCDefinition.forId(1);
        try { read.invoke(null,ByteBuffer.wrap(bytes)); throw new AssertionError("Accepted "+why); }
        catch(InvocationTargetException expected) { check(expected.getCause() instanceof IOException,why+" produces IOException"); }
        check(NPCDefinition.forId(1)==keep,"Failed decode preserves published definitions");
    }
    private static Object cacheField(CacheNPCDefinition npc,String name)throws Exception {
        Field field=CacheNPCDefinition.class.getDeclaredField(name);field.setAccessible(true);return field.get(npc);
    }
    /** Reproducible read-only input for tools/Complete-NpcCombatData.ps1. */
    private static void inventory()throws Exception {
        System.out.println("id\tname\tcombat\tsize\trender\toptions\tmodels\tchildren\tclickable\thp\tattack\tstrength\tdefence\trange\tmagic\tspeed\tattackAnim\tdefendAnim\tdeathAnim\tmelee\tranged\tmagicStyle\tprojectile");
        for(int id=0;id<Cache.getAmountOfNpcs();id++) {
            CacheNPCDefinition c=CacheNPCDefinition.forID(id);NPCDefinition d=NPCDefinition.forId(id);
            System.out.println(id+"\t"+c.name+"\t"+c.combatLevel+"\t"+c.size+"\t"+c.renderEmote+"\t"+Arrays.toString((String[])cacheField(c,"options"))+"\t"+Arrays.toString((int[])cacheField(c,"anIntArray3230"))+"\t"+Arrays.toString((int[])cacheField(c,"childrenIds"))+"\t"+cacheField(c,"isClickable")+"\t"+d.getHitpoints()+"\t"+d.getAttackLevel()+"\t"+d.getStrengthLevel()+"\t"+d.getDefenceLevel()+"\t"+d.getRangeLevel()+"\t"+d.getMagicLevel()+"\t"+d.getAttackDelay()+"\t"+d.getAttackAnimation()+"\t"+d.getDefenceAnimation()+"\t"+d.getDeathAnimation()+"\t"+d.isUsingMelee()+"\t"+d.isUsingRange()+"\t"+d.isUsingMagic()+"\t"+d.getProjectileId());
        }
    }
    private static void canonical(Element root,NPCDefinition definition)throws Exception {
        NodeList children=root.getChildNodes();
        for(int i=0;i<children.getLength();i++) {
            Node node=children.item(i);if(node.getNodeType()!=Node.ELEMENT_NODE)continue;
            String name=node.getNodeName(), fieldName=Character.toLowerCase(name.charAt(0))+name.substring(1);
            if(name.equals("LifePoints"))fieldName="lifepoints";
            if(name.equals("AttackSpeed"))fieldName="attackSpeed";
            Object actual;
            if(name.startsWith("Bonus"))actual=definition.getBonuses()[Integer.parseInt(name.substring(5))];
            else {Field field=NPCDefinition.class.getDeclaredField(fieldName);field.setAccessible(true);actual=field.get(definition);}
            check(String.valueOf(actual).equals(node.getTextContent()),"Canonical "+name+" reaches actual loader "+definition.getId());
        }
    }
    public static void main(String[] args) throws Exception {
        Cache.init();NPCDefinition.init();
        if(args.length==1&&args[0].equals("--inventory")){inventory();return;}
        byte[] current=Files.readAllBytes(Paths.get("NDE/NPCDefinitions.bin"));
        List<byte[]> now=records(current), before=records(Files.readAllBytes(Paths.get(args[0])));
        check(now.size()==Cache.getAmountOfNpcs() && now.size()==before.size(),"Complete cache slot coverage");
        Set<Integer> selected=new TreeSet<Integer>();
        for(String line:Files.readAllLines(Paths.get(args[1])))if(!line.trim().isEmpty()&&!line.startsWith("#"))selected.add(Integer.parseInt(line.trim()));
        Field options=CacheNPCDefinition.class.getDeclaredField("options");options.setAccessible(true);
        Map<Integer,String> handlers=new HashMap<Integer,String>();
        Document xml=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("data/xml/custom_npcs.xml");
        NodeList groups=xml.getElementsByTagName("npc");
        for(int i=0;i<groups.getLength();i++) {
            Element group=(Element)groups.item(i);String handler=group.getElementsByTagName("handler").item(0).getTextContent();
            NodeList ids=group.getElementsByTagName("id");
            for(int j=0;j<ids.getLength();j++){int id=Integer.parseInt(ids.item(j).getTextContent());check(handlers.put(id,handler)==null,"Unique registry id "+id);}
        }
        Method load=NPCLoader.class.getDeclaredMethod("loadCustomizations");load.setAccessible(true);load.invoke(null);
        Field registry=NPCLoader.class.getDeclaredField("CUSTOM_NPCS");registry.setAccessible(true);
        Map<?,?> actual=(Map<?,?>)registry.get(null);
        check(actual.size()==handlers.size(),"All XML IDs loaded irrespective of whitespace");
        for(Map.Entry<Integer,String> e:handlers.entrySet())check(((Class<?>)actual.get(e.getKey())).getName().equals(e.getValue()),"Registry resolves "+e.getKey());
        int added=0,modified=0,attackCandidates=0,definedAttack=0,assetWarnings=0;
        Map<Integer,String> fingerprints=new HashMap<Integer,String>();
        for(int id=0;id<now.size();id++) {
            boolean changed=!Arrays.equals(now.get(id),before.get(id));
            check(changed==selected.contains(id),"Exactly selected record changed: "+id);
            boolean attack=Arrays.asList((String[])options.get(CacheNPCDefinition.forID(id))).contains("Attack");
            if(attack){attackCandidates++;if(now.get(id).length>2)definedAttack++;}
            if(!selected.contains(id))continue;
            if(before.get(id).length==2)added++;else modified++;
            NPCDefinition d=NPCDefinition.forId(id);fingerprints.put(id,snapshot(d));
            check(d.getId()==id && d.getHitpoints()>0 && d.getAttackDelay()>0,"Usable profile "+id);
            check(d.getBonuses().length==14,"Bonus schema "+id);
            for(int seq:new int[]{d.getAttackAnimation(),d.getDefenceAnimation(),d.getDeathAnimation()}) {
                if(!asset(20,seq)){System.out.println("MISSING_SEQUENCE "+id+" "+seq);assetWarnings++;}
            }
            for(int gfx:new int[]{d.getStartGraphics(),d.getProjectileId(),d.getEndGraphics()}) {
                if(!asset(21,gfx)){System.out.println("MISSING_GRAPHIC "+id+" "+gfx);assetWarnings++;}
            }
            Element root=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse("NDE/data/NPCs/NPCDefinition"+id+".xml").getDocumentElement();
            canonical(root,d);
            if(d.isUsingRange()||d.isUsingMagic())check(d.getProjectileId()>=0||handlers.containsKey(id),"Required projectile or active script "+id);
        }
        check(assetWarnings==0,"No missing assets ("+assetWarnings+")");
        NPCDefinition.init();load.invoke(null);
        for(Map.Entry<Integer,String> e:fingerprints.entrySet())check(e.getValue().equals(snapshot(NPCDefinition.forId(e.getKey()))),"Repeat load stable "+e.getKey());
        check(NPCDefinition.forId(-2)==null && NPCDefinition.forId(now.size())==null && NPCDefinition.forId(now.size()+1)==null,"Lookup bounds");
        check(NPCDefinition.forId(-1).getName().equals("Rare drop table"),"Special -1 sentinel retained");
        byte[] wrong=current.clone();wrong[0]=0;wrong[1]=1;invalid(wrong,"wrong record ID");
        invalid(Arrays.copyOf(current,current.length-1),"truncation");invalid(Arrays.copyOf(current,current.length+1),"trailing data");
        System.out.println("NPC definition coverage: "+checks+" checks; added="+added+" modified="+modified+" exactAttack="+attackCandidates+" withRecords="+definedAttack);
    }
}
