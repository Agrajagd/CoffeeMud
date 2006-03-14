package com.planet_ink.coffee_mud.Abilities.Properties;
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
public class Prop_RideZapper extends Prop_HaveZapper
{
	public String ID() { return "Prop_RideZapper"; }
	public String name(){ return "Restrictions to riding";}
	protected int canAffectCode(){return Ability.CAN_ITEMS|Ability.CAN_MOBS;}

	public String accountForYourself()
	{
		return "Mounting restricted as follows: "+CMLib.masking().maskDesc(miscText);
	}

	public boolean okMessage(Environmental myHost, CMMsg msg)
	{
		if(affected==null) return true;
		if(!(affected instanceof Rideable)) return true;

		MOB mob=msg.source();
		if(mob.location()==null) return true;

		if(msg.amITarget(affected))
		switch(msg.targetMinor())
		{
		case CMMsg.TYP_SIT:
		case CMMsg.TYP_SLEEP:
		case CMMsg.TYP_MOUNT:
		case CMMsg.TYP_ENTER:
			if((!CMLib.masking().maskCheck(text(),mob))&&(didHappen(100)))
			{
				mob.location().show(mob,null,affected,CMMsg.MSG_OK_VISUAL,CMParms.getParmStr(text(),"MESSAGE","<O-NAME> zaps <S-NAME>, making <S-HIM-HER> jump up!"));
				return false;
			}
			break;
		}
		return true;
	}
}
