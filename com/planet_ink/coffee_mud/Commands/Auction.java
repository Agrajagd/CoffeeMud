package com.planet_ink.coffee_mud.Commands;
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
public class Auction extends Channel
{
	public Auction(){}
	protected Ability auctionA=null;

	private String[] access={"AUCTION"};
	public String[] getAccessWords(){return access;}
	public boolean execute(MOB mob, Vector commands)
		throws java.io.IOException
	{
		PlayerStats pstats=mob.playerStats();
		if(pstats==null) return false;
		int channelInt=CMLib.channels().getChannelIndex("AUCTION");
		int channelNum=CMLib.channels().getChannelCodeNumber("AUCTION");

		if(CMath.isSet(pstats.getChannelMask(),channelInt))
		{
			pstats.setChannelMask(pstats.getChannelMask()&(pstats.getChannelMask()-channelNum));
			mob.tell("The AUCTION channel has been turned on.  Use `NOAUCTION` to turn it off again.");
		}

		if((commands.size()>1)
		&&(auctionA!=null)
		&&(auctionA.invoker()==mob))
		{
			if(((String)commands.elementAt(1)).equalsIgnoreCase("CHANNEL"))
			{
				commands.removeElementAt(1);
				super.execute(mob,commands);
				return false;
			}
			else
			if(((String)commands.elementAt(1)).equalsIgnoreCase("CLOSE"))
			{
				commands.removeElementAt(1);
				Vector V=new Vector();
				V.addElement("AUCTION");
				V.addElement("The auction has been closed.");
				CMLib.threads().deleteTick(auctionA,Tickable.TICKID_QUEST);
				auctionA=null;
				super.execute(mob,V);
				return false;
			}
		}
		if(auctionA==null)
		{
			if(commands.size()==1)
			{
				mob.tell("There is nothing up for auction right now.");
				return false;
			}
			Vector V=new Vector();
			if((commands.size()>2)
			&&((CMLib.english().numPossibleGold(mob,(String)commands.lastElement())>0)||(((String)commands.lastElement()).equals("0"))))
			{
				V.addElement(commands.lastElement());
				commands.removeElementAt(commands.size()-1);
			}
			else
				V.addElement("0");

			String s=CMParms.combine(commands,1);
			Environmental E=mob.fetchInventory(null,s);
			if((E==null)||(E instanceof MOB))
			{
				mob.tell(s+" is not an item you can auction.");
				return false;
			}
			if((mob.isMonster())
            ||(!mob.session().confirm("Auction "+E.name()+" with a starting bid of "+((String)V.firstElement())+" (Y/n)?","Y")))
				return false;
			auctionA=CMClass.getAbility("Prop_Auction");
			auctionA.invoke(mob,V,E,false,0);
            auctionA.setInvoker(mob);
		}
		else
		{
			commands.removeElementAt(0);
			auctionA.invoke(mob,commands,null,false,0);
		}
		return false;
	}
	
	public boolean canBeOrdered(){return true;}

	
}
