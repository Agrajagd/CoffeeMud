package com.planet_ink.coffee_mud.WebMacros;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
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
   Copyright 2000-2010 Bo Zimmerman

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
@SuppressWarnings("unchecked")
public class FactionNext extends StdWebMacro
{
    public String name(){return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);}
    public boolean isAdminMacro()   {return true;}

    public String runMacro(ExternalHTTPRequests httpReq, String parm)
    {
        java.util.Map<String,String> parms=parseParms(parm);
        String last=httpReq.getRequestParameter("FACTION");
        if(parms.containsKey("RESET"))
        {   
            if(last!=null) httpReq.removeRequestParameter("FACTION");
            return "";
        }
        String lastID="";
        Faction F;
        String factionID;
        for(Enumeration q=CMLib.factions().factions();q.hasMoreElements();)
        {
        	F=(Faction)q.nextElement();
            factionID=F.factionID().toUpperCase().trim();
            if((last==null)||((last.length()>0)&&(last.equals(lastID))&&(!factionID.equalsIgnoreCase(lastID))))
            {
                httpReq.addRequestParameters("FACTION",factionID);
                return "";
            }
            lastID=factionID;
        }
        httpReq.addRequestParameters("FACTION","");
        if(parms.containsKey("EMPTYOK"))
            return "<!--EMPTY-->";
        return " @break@";
    }
}
