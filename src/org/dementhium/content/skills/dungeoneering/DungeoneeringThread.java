package org.dementhium.content.skills.dungeoneering;

import org.dementhium.model.map.region.RegionBuilder;

/**
 *
 * @author Ediremix
 */
public class DungeoneeringThread implements Runnable {
	
	Thread t;
	
	public static void init() {
		System.out.println("Starting seperate Dungeoneering thread..");
		new DungeoneeringThread();
	}
	
	public DungeoneeringThread() {
		t = new Thread(this);
		t.start();
	}

	@Override
	public void run() {
		for (int i = 0; i < 200; i++) {
			int[] array = {0,0};
			try {
				array = RegionBuilder.findEmptyMap(125, 125); //8*8(needed 16 by 16)
			} catch (NullPointerException e) {
				System.out.println("DungeoneeringThread Exception: "+i);
				array[0] = 0;
				array[1] = 0;
			}
			Dungeoneering.dungArray[i][0] = array[0];
			Dungeoneering.dungArray[i][1] = array[1];
 		}
		System.out.println("Loaded 200 dungeoneering maps.");
	}

}