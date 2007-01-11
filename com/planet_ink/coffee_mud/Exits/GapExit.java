package com.planet_ink.coffee_mud.Exits;
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
   Copyright 2000-2007 Bo Zimmerman

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
public class GapExit extends StdExit
{
	public String ID(){	return "GapExit";}
	public String Name(){ return "a crevasse";}
	public String description(){return "Looks like you'll have to jump it.";}

	public int mobWeight(MOB mob)
	{
		int weight=mob.baseEnvStats().weight();
		for(int i=0;i<mob.inventorySize();i++)
		{
			Item I=mob.fetchInventory(i);
			if((I!=null)&&(!I.amWearingAt(Item.WORN_FLOATING_NEARBY)))
				weight+=I.envStats().weight();
		}
		return weight;
	}

	public boolean okMessage(Environmental myHost, CMMsg msg)
	{
		if(!super.okMessage(myHost,msg)) return false;
		MOB mob=msg.source();
		if(((msg.amITarget(this))||(msg.tool()==this))
		&&(msg.targetMinor()==CMMsg.TYP_ENTER)
		&&(!CMLib.flags().isInFlight(mob))
		&&(!CMLib.flags().isFalling(mob)))
		{
			int chance=(int)Math.round(CMath.div(mobWeight(mob),mob.maxCarry())*(100.0-new Integer(3*mob.charStats().getStat(CharStats.STAT_STRENGTH)).doubleValue()));
			if(CMLib.dice().rollPercentage()<chance)
			{
				mob.location().show(mob,null,CMMsg.MSG_NOISYMOVEMENT,"<S-NAME> attempt(s) to jump the crevasse, but miss(es) the far ledge!");
				mob.location().show(mob,null,CMMsg.MSG_OK_ACTION,"<S-NAME> fall(s)!!!!");
				CMLib.combat().postDeath(null,mob,null);
				return false;
			}
		}
		return true;
	}

	public void executeMsg(Environmental myHost, CMMsg msg)
	{
		super.executeMsg(myHost,msg);
		MOB mob=msg.source();
		if(((msg.amITarget(this))||(msg.tool()==this))
		&&(msg.targetMinor()==CMMsg.TYP_ENTER)
		&&(!CMLib.flags().isInFlight(mob))
		&&(!CMLib.flags().isFalling(mob)))
			mob.location().show(mob,null,CMMsg.MSG_NOISYMOVEMENT,"<S-NAME> jump(s) the crevasse!");
	}
}
