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
   Copyright 2000-2008 Bo Zimmerman

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
public class BaseClanner extends StdCommand
{
	public static boolean goForward(MOB mob, Clan C, Vector commands, int function, boolean voteIfNecessary)
	{
		if((mob==null)||(C==null)) return false;
		int allowed=C.allowedToDoThis(mob,function);
		if(allowed==1) return true;
		if(allowed==-1) return false;
		if(function==Clan.FUNC_CLANASSIGN)
		{
			if(C.allowedToDoThis(mob,Clan.FUNC_CLANVOTEASSIGN)<=0)
			   return false;
		}
		else
		if(C.allowedToDoThis(mob,Clan.FUNC_CLANVOTEOTHER)<=0)
		   return false;
		if(!voteIfNecessary) return true;
		String matter=CMParms.combine(commands,0);
		for(Enumeration e=C.votes();e.hasMoreElements();)
		{
			Clan.ClanVote CV=(Clan.ClanVote)e.nextElement();
			if((CV.voteStarter.equalsIgnoreCase(mob.Name()))
			&&(CV.voteStatus==Clan.VSTAT_STARTED))
			{
				mob.tell("This matter must be voted upon, but you already have a vote underway.");
				return false;
			}
			if(CV.matter.equalsIgnoreCase(matter))
			{
				mob.tell("This matter must be voted upon, and is already BEING voted upon.  Use CLANVOTE to see.");
				return false;
			}
		}
		if(mob.session()==null) return false;
		try{
			int numVotes=C.getNumVoters(function);
			if(numVotes==1) return true;

			if(mob.session().confirm("This matter must be voted upon.  Would you like to start the vote now (y/N)?","N"))
			{
				Clan.ClanVote CV=new Clan.ClanVote();
				CV.matter=matter;
				CV.voteStarter=mob.Name();
				CV.function=function;
				CV.voteStarted=System.currentTimeMillis();
				CV.votes=new DVector(2);
				CV.voteStatus=Clan.VSTAT_STARTED;
				C.addVote(CV);
				C.updateVotes();
				switch(C.getGovernment())
				{
				case Clan.GVT_DEMOCRACY:
					clanAnnounce(mob,"The "+C.typeName()+" "+C.clanID()+" has a new matter to vote on. Members should use CLANVOTE to participate.");
					break;
				case Clan.GVT_DICTATORSHIP:
                case Clan.GVT_THEOCRACY:
					clanAnnounce(mob,"The "+C.typeName()+" "+C.clanID()+" has a vote -- lord only knows how.");
					break;
				case Clan.GVT_OLIGARCHY:
					clanAnnounce(mob,"The guildmasters of the "+C.typeName()+" "+C.clanID()+" have a new matter to vote upon. They should use CLANVOTE to participate.");
					break;
				case Clan.GVT_REPUBLIC:
					if(function==Clan.FUNC_CLANASSIGN)
						clanAnnounce(mob,"The "+C.typeName()+" "+C.clanID()+" has a new election to vote upon. Citizens should use CLANVOTE to participate.");
					else
						clanAnnounce(mob,"The senators of "+C.typeName()+" "+C.clanID()+" have a new matter to vote upon. They should use CLANVOTE to participate.");
					break;
				}
				mob.tell("Your vote has started.  Use CLANVOTE to cast your vote.");
				return false;
			}
		}
		catch(java.io.IOException e){}
		mob.tell("Without a vote, this command can not be executed.");
		return false;
	}


	public static void clanAnnounce(MOB mob, String msg)
	{
        Vector channels=CMLib.channels().getFlaggedChannelNames("CLANINFO");
        for(int i=0;i<channels.size();i++)
            CMLib.commands().postChannel(mob,(String)channels.elementAt(i),msg,true);
	}

	public static int getIntFromRole(int roleType)
	{
		switch(roleType)
		{
		case Clan.POS_APPLICANT: return 0;
		case Clan.POS_MEMBER: return 1;
		case Clan.POS_STAFF: return 2;
		case Clan.POS_ENCHANTER: return 3;
		case Clan.POS_TREASURER: return 4;
		case Clan.POS_LEADER: return 5;
		case Clan.POS_BOSS: return 6;

		}
		return 0;
	}
	public static int getRoleFromName(int government, String position)
	{
		if((government<0)||(government>=Clan.GVT_DESCS.length))
			government=0;
		String[] roles=Clan.ROL_DESCS[government];
		for(int i=0;i<roles.length;i++)
			if(roles[i].startsWith(position.toUpperCase()))
				return (int)CMath.pow(2,i-1);
		return -1;
	}

}
