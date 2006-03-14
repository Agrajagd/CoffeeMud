package com.planet_ink.coffee_mud.CharClasses;
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
public class PlayerClass extends StdCharClass
{
	public String ID(){return "PlayerClass";}
	public String name(){return "PlayerClass";}
	public String baseClass(){return ID();}
	private static boolean abilitiesLoaded=false;
	public boolean loaded(){return abilitiesLoaded;}
	public void setLoaded(boolean truefalse){abilitiesLoaded=truefalse;};
	
	public PlayerClass()
	{
		super();
		for(int i=0;i<CharStats.NUM_BASE_STATS;i++)
			maxStatAdj[i]=7;
	}

	public int availabilityCode(){return 0;}

	public String statQualifications(){return "";}
	public boolean qualifiesForThisClass(MOB mob, boolean quiet)
	{
		if(!quiet)
			mob.tell("This class cannot be learned.");
		return false;
	}
	
	public void startCharacter(MOB mob, boolean isBorrowedClass, boolean verifyOnly)
	{
		if((verifyOnly)&&(!loaded()))
		{
			setLoaded(true);
			for(Enumeration a=CMClass.abilities();a.hasMoreElements();)
			{
				Ability A=(Ability)a.nextElement();
				int lvl=CMLib.ableMapper().lowestQualifyingLevel(A.ID());
				if(lvl>0)
					CMLib.ableMapper().addCharAbilityMapping(ID(),1,A.ID(),false);
			}
		}
		super.startCharacter(mob, false, verifyOnly);
	}
	public void grantAbilities(MOB mob, boolean isBorrowedClass)
	{
		super.grantAbilities(mob,isBorrowedClass);
		if(mob.playerStats()==null)
		{
			Vector V=CMLib.ableMapper().getUpToLevelListings(ID(),
												mob.charStats().getClassLevel(ID()),
												false,
												false);
			for(Enumeration a=V.elements();a.hasMoreElements();)
			{
				Ability A=CMClass.getAbility((String)a.nextElement());
				if((A!=null)
				&&((A.classificationCode()&Ability.ALL_ACODES)!=Ability.ACODE_COMMON_SKILL)
				&&(!CMLib.ableMapper().getDefaultGain(ID(),true,A.ID())))
					giveMobAbility(mob,A,CMLib.ableMapper().getDefaultProfficiency(ID(),true,A.ID()),CMLib.ableMapper().getDefaultParm(ID(),true,A.ID()),isBorrowedClass);
			}
		}
	}

}
