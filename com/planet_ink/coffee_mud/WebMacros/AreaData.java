package com.planet_ink.coffee_mud.WebMacros;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
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
public class AreaData extends StdWebMacro
{
	public String name()	{return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);}


	public static StringBuffer affectsNBehaves(Environmental E, ExternalHTTPRequests httpReq, Hashtable parms)
	{
		StringBuffer str=new StringBuffer("");
		if(parms.containsKey("BEHAVIORS"))
		{
			Vector theclasses=new Vector();
			Vector theparms=new Vector();
			if(httpReq.isRequestParameter("BEHAV1"))
			{
				int num=1;
				String behav=httpReq.getRequestParameter("BEHAV"+num);
				String theparm=httpReq.getRequestParameter("BDATA"+num);
				while((behav!=null)&&(theparm!=null))
				{
					if(behav.length()>0)
					{
						theclasses.addElement(behav);
						String t=theparm;
						t=CMStrings.replaceAll(t,"\"","&quot;");
						theparms.addElement(t);
					}
					num++;
					behav=httpReq.getRequestParameter("BEHAV"+num);
					theparm=httpReq.getRequestParameter("BDATA"+num);
				}
			}
			else
			for(int b=0;b<E.numBehaviors();b++)
			{
				Behavior B=E.fetchBehavior(b);
				if((B!=null)&&(B.isSavable()))
				{
					theclasses.addElement(CMClass.className(B));
					String t=B.getParms();
					t=CMStrings.replaceAll(t,"\"","&quot;");
					theparms.addElement(t);
				}
			}
			str.append("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
			for(int i=0;i<theclasses.size();i++)
			{
				String theclass=(String)theclasses.elementAt(i);
				String theparm=(String)theparms.elementAt(i);
				str.append("<TR><TD WIDTH=50%>");
				str.append("<SELECT ONCHANGE=\"EditBehavior(this);\" NAME=BEHAV"+(i+1)+">");
				str.append("<OPTION VALUE=\"\">Delete!");
				str.append("<OPTION VALUE=\""+theclass+"\" SELECTED>"+theclass);
				str.append("</SELECT>");
				str.append("</TD><TD WIDTH=50%>");
				str.append("<INPUT TYPE=TEXT SIZE=30 NAME=BDATA"+(i+1)+" VALUE=\""+theparm+"\">");
				str.append("</TD></TR>");
			}
			str.append("<TR><TD WIDTH=50%>");
			str.append("<SELECT ONCHANGE=\"AddBehavior(this);\" NAME=BEHAV"+(theclasses.size()+1)+">");
			str.append("<OPTION SELECTED VALUE=\"\">Select a Behavior");

			Object[] sortedB=null;
			Vector sortMeB=new Vector();
			for(Enumeration b=CMClass.behaviors();b.hasMoreElements();)
			{
				Behavior B=(Behavior)b.nextElement();
				if(B.canImprove(E))
					sortMeB.addElement(CMClass.className(B));
			}
			sortedB=(new TreeSet(sortMeB)).toArray();
			for(int r=0;r<sortedB.length;r++)
			{
				String cnam=(String)sortedB[r];
				str.append("<OPTION VALUE=\""+cnam+"\">"+cnam);
			}
			str.append("</SELECT>");
			str.append("</TD><TD WIDTH=50%>");
			str.append("<INPUT TYPE=TEXT SIZE=30 NAME=BDATA"+(theclasses.size()+1)+" VALUE=\"\">");
			str.append("</TD></TR>");
			str.append("</TABLE>");
		}
		if(parms.containsKey("AFFECTS"))
		{
			Vector theclasses=new Vector();
			Vector theparms=new Vector();
			if(httpReq.isRequestParameter("AFFECT1"))
			{
				int num=1;
				String behav=httpReq.getRequestParameter("AFFECT"+num);
				String theparm=httpReq.getRequestParameter("ADATA"+num);
				while((behav!=null)&&(theparm!=null))
				{
					if(behav.length()>0)
					{
						theclasses.addElement(behav);
						String t=theparm;
						t=CMStrings.replaceAll(t,"\"","&quot;");
						theparms.addElement(t);
					}
					num++;
					behav=httpReq.getRequestParameter("AFFECT"+num);
					theparm=httpReq.getRequestParameter("ADATA"+num);
				}
			}
			else
			for(int a=0;a<E.numEffects();a++)
			{
				Ability Able=E.fetchEffect(a);
				if((Able!=null)&&(Able.savable()))
				{
					theclasses.addElement(CMClass.className(Able));
					String t=Able.text();
					t=CMStrings.replaceAll(t,"\"","&quot;");
					theparms.addElement(t);
				}
			}
			str.append("<TABLE WIDTH=100% BORDER=1 CELLSPACING=0 CELLPADDING=0>");
			for(int i=0;i<theclasses.size();i++)
			{
				String theclass=(String)theclasses.elementAt(i);
				String theparm=(String)theparms.elementAt(i);
				str.append("<TR><TD WIDTH=50%>");
				str.append("<SELECT ONCHANGE=\"EditAffect(this);\" NAME=AFFECT"+(i+1)+">");
				str.append("<OPTION VALUE=\"\">Delete!");
				str.append("<OPTION VALUE=\""+theclass+"\" SELECTED>"+theclass);
				str.append("</SELECT>");
				str.append("</TD><TD WIDTH=50%>");
				str.append("<INPUT TYPE=TEXT SIZE=30 NAME=ADATA"+(i+1)+" VALUE=\""+theparm+"\">");
				str.append("</TD></TR>");
			}
			str.append("<TR><TD WIDTH=50%>");
			str.append("<SELECT ONCHANGE=\"AddAffect(this);\" NAME=AFFECT"+(theclasses.size()+1)+">");
			str.append("<OPTION SELECTED VALUE=\"\">Select an Effect");
			for(Enumeration a=CMClass.abilities();a.hasMoreElements();)
			{
				Ability A=(Ability)a.nextElement();
				if(!A.canAffect(E)) continue;
				String cnam=A.ID();
				str.append("<OPTION VALUE=\""+cnam+"\">"+cnam);
			}
			str.append("</SELECT>");
			str.append("</TD><TD WIDTH=50%>");
			str.append("<INPUT TYPE=TEXT SIZE=30 NAME=ADATA"+(theclasses.size()+1)+" VALUE=\"\">");
			str.append("</TD></TR>");
			str.append("</TABLE>");
		}
		return str;
	}
	public String runMacro(ExternalHTTPRequests httpReq, String parm)
	{
		if(!CMProps.getBoolVar(CMProps.SYSTEMB_MUDSTARTED))
			return CMProps.getVar(CMProps.SYSTEM_MUDSTATUS);

		Hashtable parms=parseParms(parm);
		if(parms.containsKey("AREATYPES"))
		{
			StringBuffer str=new StringBuffer("");
			for(Enumeration e=CMClass.areaTypes();e.hasMoreElements();)
			{
				Area A=(Area)e.nextElement();
				str.append("<OPTION VALUE=\""+A.ID()+"\">"+A.ID());
			}
			return str.toString();
		}
		if(parms.containsKey("AREAISGRID"))
		{
			Area A=CMLib.map().getArea(""+parms.get("AREAISGRID"));
			return ""+(A instanceof GridZones);
		}
		String last=httpReq.getRequestParameter("AREA");
		if(last==null) return " @break@";

		if(last.length()>0)
		{
			Area A=CMLib.map().getArea(last);
			if(A!=null)
			{
				StringBuffer str=new StringBuffer("");
				if(parms.containsKey("HELP"))
				{
					StringBuffer s=CMLib.help().getHelpText("AREA_"+A.Name(),null,false);
					if(s==null)	s=CMLib.help().getHelpText(A.Name(),null,false);
					str.append(helpHelp(s));
				}
				if(parms.containsKey("CLIMATES"))
				{
					int climate=A.climateType();
					if(httpReq.isRequestParameter("CLIMATE"))
					{
						climate=CMath.s_int(httpReq.getRequestParameter("CLIMATE"));
						for(int i=1;;i++)
							if(httpReq.isRequestParameter("CLIMATE"+(new Integer(i).toString())))
								climate=climate|CMath.s_int(httpReq.getRequestParameter("CLIMATE"+(new Integer(i).toString())));
							else
								break;
					}
					for(int i=1;i<Area.NUM_CLIMATES;i++)
					{
						String climstr=Area.CLIMATE_DESCS[i];
						int mask=(int)CMath.pow(2,i-1);
						str.append("<OPTION VALUE="+mask);
						if((climate&mask)>0) str.append(" SELECTED");
						str.append(">"+climstr);
					}
				}
				if(parms.containsKey("TECHLEVEL"))
				{
					for(int i=1;i<Area.THEME_DESCS.length;i++)
					{
						str.append("<OPTION VALUE="+i);
						if(i==A.getTechLevel()) str.append(" SELECTED");
						str.append(">"+Area.THEME_DESCS[i]);
					}
				}
				if(parms.containsKey("NAME"))
				{
					String name=httpReq.getRequestParameter("NAME");
					if((name==null)||(name.length()==0))
						name=A.Name();
					str.append(name);
				}
				if(parms.containsKey("IMAGE"))
				{
					String name=httpReq.getRequestParameter("IMAGE");
					if((name==null)||(name.length()==0))
						name=A.rawImage();
					str.append(name);
				}
				if((parms.containsKey("GRIDX"))&&(A instanceof GridZones))
				{
					String name=httpReq.getRequestParameter("GRIDX");
					if((name==null)||(name.length()==0))
						name=""+((GridZones)A).xGridSize();
					str.append(name);
				}
				if((parms.containsKey("GRIDY"))&&(A instanceof GridZones))
				{
					String name=httpReq.getRequestParameter("GRIDY");
					if((name==null)||(name.length()==0))
						name=""+((GridZones)A).yGridSize();
					str.append(name);
				}
				if(parms.containsKey("ISGRID"))
					str.append(""+(A instanceof GridZones));
				if(parms.containsKey("AUTHOR"))
				{
					String author=httpReq.getRequestParameter("AUTHOR");
					if((author==null)||(author.length()==0))
						author=A.getAuthorID();
					str.append(author);
				}
				if(parms.containsKey("ARCHP"))
				{
					String path=httpReq.getRequestParameter("ARCHP");
					if((path==null)||(path.length()==0))
						path=A.getArchivePath();
					str.append(path);
				}
				if(parms.containsKey("CURRENCIES"))
				{
					str.append("<OPTION VALUE=\"\"");
					if(A.getCurrency().length()==0) str.append(" SELECTED");
					str.append(">Default Currency");
					for(int i=1;i<CMLib.beanCounter().getAllCurrencies().size();i++)
					{
					    String s=(String)CMLib.beanCounter().getAllCurrencies().elementAt(i);
					    if(s.length()>0)
					    {
							str.append("<OPTION VALUE=\""+s+"\"");
							if(s.equalsIgnoreCase(A.getCurrency())) str.append(" SELECTED");
							str.append(">"+s);
					    }
					}
				}
				if(parms.containsKey("CURRENCY"))
				{
					String currency=httpReq.getRequestParameter("CURRENCY");
					if((currency==null)||(currency.length()==0))
					    currency=A.getCurrency();
					str.append(currency);
				}
				if(parms.containsKey("CLASSES"))
				{
					String className=httpReq.getRequestParameter("CLASS");
					if((className==null)||(className.length()==0))
						className=CMClass.className(A);
					Object[] sortedA=(Object[])Resources.getResource("MUDGRINDER-AREAS");
					if(sortedA==null)
					{
						Vector sortMeA=new Vector();
						for(Enumeration a=CMClass.areaTypes();a.hasMoreElements();)
							sortMeA.addElement(CMClass.className(a.nextElement()));
						sortedA=(new TreeSet(sortMeA)).toArray();
						Resources.submitResource("MUDGRINDER-AREAS",sortedA);
					}
					for(int r=0;r<sortedA.length;r++)
					{
						String cnam=(String)sortedA[r];
						str.append("<OPTION VALUE=\""+cnam+"\"");
						if(className.equals(cnam))
							str.append(" SELECTED");
						str.append(">"+cnam);
					}
				}
				if(parms.containsKey("TESTSTUFF"))
					str.append(A.text());

				str.append(AreaData.affectsNBehaves(A,httpReq,parms));

				if(parms.containsKey("SUBOPS"))
				{
					String subOps=httpReq.getRequestParameter("SUBOPS");
					if((subOps==null)||(subOps.length()==0))
						subOps=A.getSubOpList();
					else
					for(int i=1;;i++)
						if(httpReq.isRequestParameter("SUBOPS"+(new Integer(i).toString())))
							subOps+=";"+httpReq.getRequestParameter("SUBOPS"+(new Integer(i).toString()));
						else
							break;
					Vector V=CMLib.database().userList();
					for(int v=0;v<V.size();v++)
					{
						String cnam=(String)V.elementAt(v);
						str.append("<OPTION VALUE=\""+cnam+"\"");
						if(subOps.equals(cnam)
						   ||(subOps.indexOf(";"+cnam)>=0)
						   ||(subOps.startsWith(cnam+";")))
							str.append(" SELECTED");
						str.append(">"+cnam);
					}
				}
				if(parms.containsKey("DESCRIPTION"))
				{
					String desc=httpReq.getRequestParameter("DESCRIPTION");
					if((desc==null)||(desc.length()==0))
						desc=A.description();
					str.append(desc);
				}

                if(parms.containsKey("PARENT"))
                {
                    String parents=httpReq.getRequestParameter("PARENT");
                    if((parents==null)||(parents.length()==0))
                        parents=A.getParentsList();
                    else
                    for(int i=1;;i++)
                        if(httpReq.isRequestParameter("PARENT"+(new Integer(i).toString())))
                            parents+=";"+httpReq.getRequestParameter("PARENT"+(new Integer(i).toString()));
                        else
                            break;
                    for(Enumeration e=CMLib.map().areas();e.hasMoreElements();)
                    {
                        String cnam=((Area)e.nextElement()).Name();
                        str.append("<OPTION VALUE=\""+cnam+"\"");
                        if(parents.equals(cnam)
                           ||(parents.indexOf(";"+cnam)>=0)
                           ||(parents.startsWith(cnam+";")))
                                str.append(" SELECTED");
                        str.append(">"+cnam);
                    }
                }

                if(parms.containsKey("CHILDREN"))
                {
                    String children=httpReq.getRequestParameter("CHILDREN");
                    if((children==null)||(children.length()==0))
                        children=A.getChildrenList();
                    else
                    for(int i=1;;i++)
                        if(httpReq.isRequestParameter("CHILDREN"+(new Integer(i).toString())))
                            children+=";"+httpReq.getRequestParameter("CHILDREN"+(new Integer(i).toString()));
                        else
                            break;
                    for(Enumeration e=CMLib.map().areas();e.hasMoreElements();)
                    {
                        String cnam=((Area)e.nextElement()).Name();
                        str.append("<OPTION VALUE=\""+cnam+"\"");
                        if(children.equals(cnam)
                           ||(children.indexOf(";"+cnam)>=0)
                           ||(children.startsWith(cnam+";")))
                                str.append(" SELECTED");
                        str.append(">"+cnam);
                    }
                }


				if(parms.containsKey("SEASON"))
					str.append(TimeClock.SEASON_DESCS[A.getTimeObj().getSeasonCode()]+", ");
				if(parms.containsKey("TODCODE"))
					str.append(TimeClock.TOD_DESC[A.getTimeObj().getTODCode()]+", ");
				if(parms.containsKey("WEATHER"))
					str.append(A.getClimateObj().getWeatherDescription(A)+", ");
				if(parms.containsKey("MOON"))
					str.append(TimeClock.MOON_PHASES[A.getTimeObj().getMoonPhase()]+", ");
				if(parms.containsKey("STATS"))
					str.append(A.getAreaStats()+", ");
				String strstr=str.toString();
				if(strstr.endsWith(", "))
					strstr=strstr.substring(0,strstr.length()-2);
                return clearWebMacros(strstr);
			}
		}
		return "";
	}
}
