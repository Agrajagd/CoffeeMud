package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.Area.Stats;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.ListingLibrary;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2000-2014 Bo Zimmerman

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
public class Areas extends StdCommand
{
	public Areas(){}

	private final String[] access=_i(new String[]{"AREAS"});
	@Override public String[] getAccessWords(){return access;}
	@Override
	public boolean execute(MOB mob, Vector commands, int metaFlags)
		throws java.io.IOException
	{
		String expression=null;
		Enumeration<Area> a=CMLib.map().areas();
		int addStat=-1;
		String append="";

		for(int i=1;i<commands.size();i++)
		{
			final String s=(String)commands.elementAt(i);
			if(s.toUpperCase().startsWith("SORT=NA"))
			{
				append = " (sorted by name)";
				commands.remove(i);
				i--;
			}
			else
			if(s.toUpperCase().startsWith("SORT=REV"))
			{
				final TreeSet<Area> levelSorted=new TreeSet<Area>(new Comparator<Area>()
				{
					@Override
					public int compare(Area arg0, Area arg1)
					{
						return arg1.Name().compareTo(arg0.Name());
					}
				});
				for(;a.hasMoreElements();) levelSorted.add(a.nextElement());
				a=new IteratorEnumeration<Area>(levelSorted.iterator());
				append = " (sorted by name, reverse)";
				commands.remove(i);
				i--;
			}
			else
			if(s.toUpperCase().startsWith("SORT=LEV"))
			{
				final TreeSet<Area> levelSorted=new TreeSet<Area>(new Comparator<Area>()
				{
					@Override
					public int compare(Area arg0, Area arg1)
					{
						final int lvl1=arg0.getAreaIStats()[Stats.MED_LEVEL.ordinal()];
						final int lvl2=arg1.getAreaIStats()[Stats.MED_LEVEL.ordinal()];
						if(lvl1==lvl2) return 1;
						return Integer.valueOf(lvl1).compareTo(Integer.valueOf(lvl2));
					}
				});
				for(;a.hasMoreElements();) levelSorted.add(a.nextElement());
				a=new IteratorEnumeration<Area>(levelSorted.iterator());
				append = " (sorted by level)";
				addStat=Stats.MED_LEVEL.ordinal();
				commands.remove(i);
				i--;
			}
			else
			if(s.toUpperCase().startsWith("SORT="))
			{
				int statVal=-1;
				for(int x=0;x<Area.Stats.values().length;x++)
					if(s.toUpperCase().endsWith("="+Area.Stats.values()[x].name()))
						statVal=x;
				if(statVal<0)
				{
					mob.tell(_("There was an error in your SORT= qualifier: '@x1' is unknown.",s.substring(5)));
					return false;
				}
				final int sortStat=statVal;
				final TreeSet<Area> levelSorted=new TreeSet<Area>(new Comparator<Area>()
				{
					@Override
					public int compare(Area arg0, Area arg1)
					{
						final int lvl1=arg0.getAreaIStats()[sortStat];
						final int lvl2=arg1.getAreaIStats()[sortStat];
						if(lvl1==lvl2) return 1;
						return Integer.valueOf(lvl1).compareTo(Integer.valueOf(lvl2));
					}
				});
				for(;a.hasMoreElements();) levelSorted.add(a.nextElement());
				a=new IteratorEnumeration<Area>(levelSorted.iterator());
				append = " (sorted by "+Area.Stats.values()[statVal].name().toLowerCase()+")";
				addStat=sortStat;
				commands.remove(i);
				i--;
			}
		}

		StringBuffer msg=new StringBuffer(_("^HComplete areas list@x1:^?^N\n\r",append));
		if(commands.size()>1)
		{
			expression=CMParms.combineWithQuotes(commands,1);
			msg=new StringBuffer(_("^HFiltered areas list@x1:^?^N\n\r",append));
		}
		final Vector areasVec=new Vector();
		final boolean sysop=(mob!=null)&&CMSecurity.isASysOp(mob);
		final int colWidth=ListingLibrary.ColFixer.fixColWidth(22.0,mob);
		for(;a.hasMoreElements();)
		{
			final Area A=a.nextElement();
			if(CMLib.flags().canAccess(mob,A)&&(!CMath.bset(A.flags(),Area.FLAG_INSTANCE_CHILD))&&(!(A instanceof SpaceObject)))
			{
				final String levelStr = (addStat>=0?(Integer.toString(A.getAreaIStats()[addStat])+":"):"");
				String name=levelStr+((!CMLib.flags().isHidden(A))?" "+A.name():"("+A.name()+")");
				if(sysop)
				switch(A.getAreaState())
				{
				case ACTIVE: name="^w"+name+"^?";break;
				case PASSIVE: name="^W"+name+"^?"; break;
				case FROZEN: name="^b"+name+"^?"; break;
				case STOPPED: name="^r"+name+"^?"; break;
				}
				if(expression!=null)
				{
					final int[] stats=A.getAreaIStats();
					if(stats!=null)
					{
						final Hashtable H=new Hashtable();
						for(int i=0;i<stats.length;i++)
							H.put(Area.Stats.values()[i].name(),Integer.toString(stats[i]));
						try
						{
							if(!CMStrings.parseStringExpression(expression, H,false))
								continue;
						}catch(final Exception e)
						{
							if(mob!=null)
								mob.tell(_("There was an error in your AREA qualifier parameters. See help on AREA for more information. The error was: @x1",e.getMessage()));
							return false;
						}
					}
				}
				areasVec.addElement(name);
			}
		}
		int col=0;
		for(int i=0;i<areasVec.size();i++)
		{
			if((++col)>3)
			{
				msg.append("\n\r");
				col=1;
			}
			msg.append(CMStrings.padRight((String)areasVec.elementAt(i),colWidth)+"^N");
		}
		msg.append(_("\n\r\n\r^HEnter 'HELP (AREA NAME) for more information.^?"));
		if((mob!=null)&&(!mob.isMonster()))
			mob.session().colorOnlyPrintln(msg.toString());
		return false;
	}

	@Override public boolean canBeOrdered(){return true;}


}
