package com.planet_ink.coffee_mud.Races;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;


import java.util.*;

/* 
   Copyright 2000-2006 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class Nymph extends StdRace
{
	public String ID(){	return "Nymph"; }
	public String name(){ return "Nymph"; }
	public int shortestMale(){return 64;}
	public int shortestFemale(){return 59;}
	public int heightVariance(){return 12;}
	public int lightestWeight(){return 90;}
	public int weightVariance(){return 90;}
	public long forbiddenWornBits(){return 0;}
	public String racialCategory(){return "Fairy-kin";}
	private String[]culturalAbilityNames={"Fey"};
	private int[]culturalAbilityProfficiencies={50};
	public String[] culturalAbilityNames(){return culturalAbilityNames;}
	public int[] culturalAbilityProfficiencies(){return culturalAbilityProfficiencies;}
	private String[]racialAbilityNames={"Spell_Charm"};
	private int[]racialAbilityLevels={1};
	private int[]racialAbilityProfficiencies={100};
	private boolean[]racialAbilityQuals={false};
	protected String[] racialAbilityNames(){return racialAbilityNames;}
	protected int[] racialAbilityLevels(){return racialAbilityLevels;}
	protected int[] racialAbilityProfficiencies(){return racialAbilityProfficiencies;}
	protected boolean[] racialAbilityQuals(){return racialAbilityQuals;}

	//                                an ey ea he ne ar ha to le fo no gi mo wa ta wi
	private static final int[] parts={0 ,2 ,2 ,1 ,1 ,2 ,2 ,1 ,2 ,2 ,1 ,0 ,1 ,1 ,0 ,0 };
	public int[] bodyMask(){return parts;}

	private int[] agingChart={0,2,20,110,175,263,350,390,430};
	public int[] getAgingChart(){return agingChart;}
	
	protected static Vector resources=new Vector();
	public int availabilityCode(){return Area.THEME_FANTASY|Area.THEME_SKILLONLYMASK;}

	public void startRacing(MOB mob, boolean verifyOnly)
	{
		super.startRacing(mob,verifyOnly);
		if(!verifyOnly)
			mob.baseEnvStats().setSensesMask(EnvStats.CAN_SEE_INFRARED);
	}
	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		affectableStats.setSensesMask(affectableStats.sensesMask()|EnvStats.CAN_SEE_INFRARED);
	}
	public void affectCharStats(MOB affectedMOB, CharStats affectableStats)
	{
		super.affectCharStats(affectedMOB, affectableStats);
		affectableStats.setStat(CharStats.STAT_SAVE_MAGIC,affectableStats.getStat(CharStats.STAT_SAVE_MAGIC)+5);
		affectableStats.setStat(CharStats.STAT_SAVE_JUSTICE,affectableStats.getStat(CharStats.STAT_SAVE_JUSTICE)+5);
		affectableStats.setStat(CharStats.STAT_GENDER,'F');
	}
	public Vector outfit(MOB myChar)
	{
		if(outfitChoices==null)
		{
			outfitChoices=new Vector();
			// Have to, since it requires use of special constructor
			Armor s1=CMClass.getArmor("GenShirt");
			s1.setName("a delicate green shawl");
			s1.setDisplayText("a delicate green shawl sits gracefully here.");
			s1.setDescription("Obviously fine craftmenship, with delicate folds and intricate designs.");
			s1.text();
			outfitChoices.addElement(s1);

			Armor s2=CMClass.getArmor("GenShoes");
			s2.setName("a pair of sandals");
			s2.setDisplayText("a pair of sandals lie here.");
			s2.setDescription("Obviously fine craftmenship, these light leather sandals have tiny woodland drawings in them.");
			s2.text();
			outfitChoices.addElement(s2);

			Armor p1=CMClass.getArmor("GenPants");
			p1.setName("a delicate skirt");
			p1.setDisplayText("a short thin skirt sits here.");
			p1.setDescription("Obviously fine craftmenship, with delicate folds and intricate designs.  It looks very alluring!");
			p1.text();
			outfitChoices.addElement(p1);
			
			Armor s3=CMClass.getArmor("GenBelt");
			outfitChoices.addElement(s3);
		}
		return outfitChoices;
	}
	public Weapon myNaturalWeapon()
	{ return funHumanoidWeapon();	}

	public String healthText(MOB mob)
	{
		double pct=(CMath.div(mob.curState().getHitPoints(),mob.maxState().getHitPoints()));

		if(pct<.10)
			return "^r" + mob.name() + "^r is mortally wounded and will soon die.^N";
		else
		if(pct<.20)
			return "^r" + mob.name() + "^r is covered in blood.^N";
		else
		if(pct<.30)
			return "^r" + mob.name() + "^r is bleeding badly from lots of wounds.^N";
		else
		if(pct<.40)
			return "^y" + mob.name() + "^y has numerous bloody wounds and gashes.^N";
		else
		if(pct<.50)
			return "^y" + mob.name() + "^y has some bloody wounds and gashes.^N";
		else
		if(pct<.60)
			return "^p" + mob.name() + "^p has a few bloody wounds.^N";
		else
		if(pct<.70)
			return "^p" + mob.name() + "^p is cut and bruised.^N";
		else
		if(pct<.80)
			return "^g" + mob.name() + "^g has some minor cuts and bruises.^N";
		else
		if(pct<.90)
			return "^g" + mob.name() + "^g has a few bruises and scratches.^N";
		else
		if(pct<.99)
			return "^g" + mob.name() + "^g has a few small bruises.^N";
		else
			return "^c" + mob.name() + "^c is in perfect health.^N";
	}
	public Vector myResources()
	{
		synchronized(resources)
		{
			if(resources.size()==0)
			{
				resources.addElement(makeResource
				("some "+name().toLowerCase()+" hair",RawMaterial.RESOURCE_FUR));
				resources.addElement(makeResource
				("some "+name().toLowerCase()+" blood",RawMaterial.RESOURCE_BLOOD));
				resources.addElement(makeResource
				("a pile of "+name().toLowerCase()+" bones",RawMaterial.RESOURCE_BONE));
			}
		}
		return resources;
	}
}
