package com.planet_ink.coffee_mud.WebMacros.grinder;
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
public class GrinderAreas
{
	public static String getAreaList(Area pickedA, MOB mob)
	{
		StringBuffer AreaList=new StringBuffer("");
		boolean anywhere=(CMSecurity.isAllowedAnywhere(mob,"CMDROOMS")||CMSecurity.isAllowedAnywhere(mob,"CMDAREAS"));
		boolean everywhere=(CMSecurity.isASysOp(mob)||CMSecurity.isAllowedEverywhere(mob,"CMDROOMS")||CMSecurity.isAllowedEverywhere(mob,"CMDAREAS"));
		for(Enumeration a=CMLib.map().areas();a.hasMoreElements();)
		{
			Area A=(Area)a.nextElement();
			if(everywhere||(A.amISubOp(mob.Name())&&anywhere))
				if((pickedA!=null)&&(pickedA==A))
					AreaList.append("<OPTION SELECTED VALUE=\""+A.Name()+"\">"+A.name());
				else
					AreaList.append("<OPTION VALUE=\""+A.Name()+"\">"+A.name());
		}
		return AreaList.toString();
	}

	public static String doAffectsNBehavs(Environmental E, ExternalHTTPRequests httpReq, Hashtable parms)
	{
		while(E.numBehaviors()>0)
			E.delBehavior(E.fetchBehavior(0));
		if(httpReq.isRequestParameter("BEHAV1"))
		{
			int num=1;
			String behav=httpReq.getRequestParameter("BEHAV"+num);
			String theparm=httpReq.getRequestParameter("BDATA"+num);
			while((behav!=null)&&(theparm!=null))
			{
				if(behav.length()>0)
				{
					Behavior B=CMClass.getBehavior(behav);
					if(theparm==null) theparm="";
					if(B==null) return "Unknown behavior '"+behav+"'.";
					B.setParms(theparm);
					E.addBehavior(B);
					B.startBehavior(E);
				}
				num++;
				behav=httpReq.getRequestParameter("BEHAV"+num);
				theparm=httpReq.getRequestParameter("BDATA"+num);
			}
		}
		while(E.numEffects()>0)
			E.delEffect(E.fetchEffect(0));
		if(httpReq.isRequestParameter("AFFECT1"))
		{
			int num=1;
			String aff=httpReq.getRequestParameter("AFFECT"+num);
			String theparm=httpReq.getRequestParameter("ADATA"+num);
			while((aff!=null)&&(theparm!=null))
			{
				if(aff.length()>0)
				{
					Ability B=CMClass.getAbility(aff);
					if(theparm==null) theparm="";
					if(B==null) return "Unknown Effect '"+aff+"'.";
					B.setMiscText(theparm);
					E.addNonUninvokableEffect(B);
				}
				num++;
				aff=httpReq.getRequestParameter("AFFECT"+num);
				theparm=httpReq.getRequestParameter("ADATA"+num);
			}
		}
		return "";
	}

	public static String modifyArea(ExternalHTTPRequests httpReq, Hashtable parms)
	{
        Vector areasNeedingUpdates=new Vector();
		String last=httpReq.getRequestParameter("AREA");
		if((last==null)||(last.length()==0)) return "Old area name not defined!";
		Area A=CMLib.map().getArea(last);
		if(A==null) return "Old Area not defined!";
		areasNeedingUpdates.addElement(A);

		boolean redoAllMyDamnRooms=false;
		Vector allMyDamnRooms=null;
		String oldName=null;

		// class!
		String className=httpReq.getRequestParameter("CLASSES");
		if((className==null)||(className.length()==0))
			return "Please select a class type for this area.";
		if(!className.equalsIgnoreCase(CMClass.className(A)))
		{
			allMyDamnRooms=new Vector();
			for(Enumeration r=A.getProperMap();r.hasMoreElements();)
				allMyDamnRooms.addElement(r.nextElement());
			Area oldA=A;
			A=CMClass.getAreaType(className);
			if(A==null)
				return "The class you chose does not exist.  Choose another.";
			CMLib.map().delArea(oldA);
			CMLib.map().addArea(A);
			A.setName(oldA.Name());
			redoAllMyDamnRooms=true;
		}

		// name
		String name=httpReq.getRequestParameter("NAME");
		if((name==null)||(name.length()==0))
			return "Please enter a name for this area.";
		name=name.trim();
		if(!name.equals(A.Name().trim()))
		{
			if(CMLib.map().getArea(name)!=null)
				return "The name you chose is already in use.  Please enter another.";
			allMyDamnRooms=new Vector();
			for(Enumeration r=A.getCompleteMap();r.hasMoreElements();)
				allMyDamnRooms.addElement(r.nextElement());
			CMLib.map().delArea(A);
			oldName=A.Name();
			CMLib.database().DBDeleteArea(A);
			A=CMLib.database().DBCreateArea(name,CMClass.className(A));
			A.setName(name);
			redoAllMyDamnRooms=true;
			httpReq.addRequestParameters("AREA",A.Name());
		}

		// climate
		if(httpReq.isRequestParameter("CLIMATE"))
		{
			int climate=CMath.s_int(httpReq.getRequestParameter("CLIMATE"));
			for(int i=1;;i++)
				if(httpReq.isRequestParameter("CLIMATE"+(new Integer(i).toString())))
					climate=climate|CMath.s_int(httpReq.getRequestParameter("CLIMATE"+(new Integer(i).toString())));
				else
					break;
			A.setClimateType(climate);
		}

		// tech level
		if(httpReq.isRequestParameter("TECHLEVEL"))
			A.setClimateType(CMath.s_int(httpReq.getRequestParameter("TECHLEVEL")));

		// modify subop list
		String subOps=httpReq.getRequestParameter("SUBOPS");
		Vector V=A.getSubOpVectorList();
		for(int v=0;v<V.size();v++)
			A.delSubOp((String)V.elementAt(v));
		if((subOps!=null)&&(subOps.length()>0))
		{
			A.addSubOp(subOps);
			for(int i=1;;i++)
				if(httpReq.isRequestParameter("SUBOPS"+(new Integer(i).toString())))
					A.addSubOp(httpReq.getRequestParameter("SUBOPS"+(new Integer(i).toString())));
				else
					break;
		}

		// description
		String desc=httpReq.getRequestParameter("DESCRIPTION");
		if(desc==null)desc="";
		A.setDescription(CMLib.coffeeFilter().safetyFilter(desc));

		// image
		String img=httpReq.getRequestParameter("IMAGE");
		if(img==null)img="";
		A.setImage(CMLib.coffeeFilter().safetyFilter(img));
		
		// gridy
		String gridy=httpReq.getRequestParameter("GRIDY");
		if((gridy!=null)&&(A instanceof GridZones))
			((GridZones)A).setYGridSize(CMath.s_int(gridy));
		// gridx
		String gridx=httpReq.getRequestParameter("GRIDX");
		if((gridx!=null)&&(A instanceof GridZones))
			((GridZones)A).setXGridSize(CMath.s_int(gridy));
		
		// author
		String author=httpReq.getRequestParameter("AUTHOR");
		if(author==null)author="";
		A.setAuthorID(CMLib.coffeeFilter().safetyFilter(author));
		
		// currency
		String currency=httpReq.getRequestParameter("CURRENCY");
		if(currency==null)currency="";
		A.setCurrency(CMLib.coffeeFilter().safetyFilter(currency));
		
        // modify Child Area list
        String parents=httpReq.getRequestParameter("PARENT");
        for(int v=0;v<A.getNumParents();v++)
            A.removeParent(v);
        if((parents!=null)&&(parents.length()>0))
        {
            Area parent=CMLib.map().getArea(parents);
            if(parent!=null)
			{
                if(A.canParent(parent))
				{
                    A.addParent(parent);
                    parent.addChild(A);
                    areasNeedingUpdates.addElement(parent);
                }
                else
                    return "The area, '"+parent.Name()+"', cannot be added as a parent, as this would create a circular reference.";
            }
            for(int i=1;;i++)
                if(httpReq.isRequestParameter("PARENT"+(new Integer(i).toString())))
				{
                    parent=CMLib.map().getArea(httpReq.getRequestParameter("PARENT"+(new Integer(i).toString())));
                    if(parent==null)
						Log.errOut("Grinder", "Error - Area '"+httpReq.getRequestParameter("PARENT"+(new Integer(i).toString()))+"' not found by CMMap");
                    else
					{
						if(A.canParent(parent))
						{
							A.addParent(parent);
							parent.addChild(A);
							areasNeedingUpdates.addElement(parent);
						}
						else
						    return "The area, '"+parent.Name()+"', cannot be added as a parent, as this would create a circular reference.";
                    }
                }
				else
					break;
        }

        // modify Child Area list
        String children=httpReq.getRequestParameter("CHILDREN");
        for(int v=0;v<A.getNumChildren();v++)
            A.removeChild(v);
        if((children!=null)&&(children.length()>0))
        {
			Area child=CMLib.map().getArea(children);
			if(child!=null)
			{
				if(A.canChild(child))
				{
				    A.addChild(child);
				    child.addParent(A);
				    areasNeedingUpdates.addElement(child);
				}
				else
				    return "The area, '"+child.Name()+"', cannot be added as a child, as this would create a circular reference.";
			}
			for(int i=1;;i++)
			    if(httpReq.isRequestParameter("CHILDREN"+(new Integer(i).toString())))
				{
			        child=CMLib.map().getArea(httpReq.getRequestParameter("CHILDREN"+(new Integer(i).toString())));
			        if(child==null)
						Log.errOut("Grinder", "Error - Area '"+httpReq.getRequestParameter("CHILDREN"+(new Integer(i).toString()))+"' not found by CMMap");
			        else
					{
			            if(A.canChild(child))
						{
			                A.addChild(child);
			                child.addParent(A);
			                areasNeedingUpdates.addElement(child);
			            }
			            else
			                return "The area, '"+child.Name()+"', cannot be added as a child, as this would create a circular reference.";
			        }
			    }
				else
					break;
        }

		// archive file
		String file=httpReq.getRequestParameter("ARCHP");
		if(file==null)file="";
		A.setArchivePath(file);

		String err=GrinderAreas.doAffectsNBehavs(A,httpReq,parms);
		if(err.length()>0) return err;

		if((redoAllMyDamnRooms)&&(allMyDamnRooms!=null))
			CMLib.map().renameRooms(A,oldName,allMyDamnRooms);

		for(int i=0;i<areasNeedingUpdates.size();i++) // will always include A
		{
		    Area A2=(Area)areasNeedingUpdates.elementAt(i);
			CMLib.database().DBUpdateArea(A2.Name(),A2);
            CMLib.coffeeMaker().addAutoPropsToAreaIfNecessary(A2);
		}
		return "";
	}
}
