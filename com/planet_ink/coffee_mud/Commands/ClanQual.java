package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.Clan.Authority;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/* 
   Copyright 2000-2012 Bo Zimmerman

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
@SuppressWarnings({"unchecked","rawtypes"})
public class ClanQual extends StdCommand
{
	public ClanQual(){}

	private final String[] access={"CLANQUAL"};
	public String[] getAccessWords(){return access;}
	public boolean execute(MOB mob, Vector commands, int metaFlags)
		throws java.io.IOException
	{
		String clanName=(commands.size()>1)?CMParms.combine(commands,1,commands.size()):"";

		Clan C=null;
		boolean skipChecks=mob.getClanRole(mob.Name())!=null;
		if(skipChecks) C=mob.getClanRole(mob.Name()).first;
			
		if(C==null)
		for(Pair<Clan,Integer> c : mob.clans())
			if((clanName.length()==0)||(CMLib.english().containsString(c.first.getName(), clanName))
			&&(c.first.getAuthority(c.second.intValue(), Clan.Function.PREMISE)!=Authority.CAN_NOT_DO))
			{	C=c.first; break; }
		
		commands.setElementAt(getAccessWords()[0],0);

		if(C==null)
		{
			mob.tell("You aren't allowed to set qualifications for "+((clanName.length()==0)?"anything":clanName)+".");
			return false;
		}

		StringBuffer msg=new StringBuffer("");
		if((!skipChecks)&&(!CMLib.clans().goForward(mob,C,commands,Clan.Function.PREMISE,false)))
		{
			msg.append("You aren't in the right position to set the qualifications to your "+C.getGovernmentName()+".");
		}
		else
		{
			try
			{
				String premise="?";
				while(premise.equals("?"))
				{
					if((skipChecks)&&(commands.size()>1))
						premise=CMParms.combine(commands,1);
					else
					if(mob.session()!=null)
						premise=mob.session().prompt("Describe your "+C.getGovernmentName()+"'s Qualification Code (?)\n\r: ","");

					if(premise.equals("?"))
						mob.tell(CMLib.masking().maskHelp("\n\r","disallow"));
					else
					if(premise.length()>0)
					{
						mob.tell("Your qualifications will be as follows: "+CMLib.masking().maskDesc(premise)+"\n\r");
						if((mob.session()!=null)&&(mob.session().confirm("Is this correct (Y/n)?","Y")))
						{
							commands.clear();
							commands.addElement(getAccessWords()[0]);
							commands.addElement(premise);
							if(skipChecks||CMLib.clans().goForward(mob,C,commands,Clan.Function.PREMISE,true))
							{
								C.setAcceptanceSettings(premise);
								C.update();
								CMLib.clans().clanAnnounce(mob,"The qualifications of "+C.getGovernmentName()+" "+C.clanID()+" have been changed.");
								return false;
							}
						}
						else
							premise="?";
					}
					else
						return false;
				}
			}
			catch(java.io.IOException e)
			{
			}
		}
		mob.tell(msg.toString());
		return false;
	}
	
	public boolean canBeOrdered(){return false;}

	
}
