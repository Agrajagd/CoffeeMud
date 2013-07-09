package com.planet_ink.coffee_mud.Abilities.Spells;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
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
   Copyright 2000-2013 Bo Zimmerman

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
@SuppressWarnings("rawtypes")
public class Spell_AcidArrow extends Spell
{
	public String ID() { return "Spell_AcidArrow"; }
	public String name(){ return "Acid Arrow";}
	public String displayText(){ return "(Acid Arrow)";}
	public int abstractQuality(){ return Ability.QUALITY_MALICIOUS;}
	protected int canAffectCode(){return CAN_MOBS;}
	protected int canTargetCode(){return 0;}
	public int maxRange(){return adjustedMaxInvokerRange(2);}
	public int classificationCode(){ return Ability.ACODE_SPELL|Ability.DOMAIN_CONJURATION;}
	public long flags(){return Ability.FLAG_EARTHBASED;}

	public boolean tick(Tickable ticking, int tickID)
	{
		if((tickID==Tickable.TICKID_MOB)
		&&(affected!=null)
		&&(affected instanceof MOB))
		{
			MOB vic=(MOB)affected;
			if((!vic.amDead())&&(vic.location()!=null))
				CMLib.combat().postDamage(invoker,vic,this,CMLib.dice().roll(2,4+super.getXLEVELLevel(invoker())+(2*super.getX1Level(invoker())),0),CMMsg.MASK_ALWAYS|CMMsg.TYP_ACID,-1,"<T-NAME> sizzle(s) from the acid arrow residue!");
		}
		return super.tick(ticking,tickID);
	}
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null) return false;
		Room R=CMLib.map().roomLocation(target);
		if(R==null) R=mob.location();

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			CMMsg msg=CMClass.getMsg(mob,target,this,somanticCastCode(mob,target,auto),(auto?"An arrow made of acid appears zooming towards <T-NAME>!":"^S<S-NAME> point(s) at <T-NAMESELF>, conjuring an acid arrow!^?")+CMLib.protocol().msp("spelldam1.wav",40));
			CMMsg msg2=CMClass.getMsg(mob,target,this,CMMsg.MSK_CAST_MALICIOUS_VERBAL|CMMsg.TYP_ACID|(auto?CMMsg.MASK_ALWAYS:0),null);
			if((R.okMessage(mob,msg))&&((R.okMessage(mob,msg2))))
			{
				R.send(mob,msg);
				R.send(mob,msg2);
				invoker=mob;
				int numDice = (adjustedLevel(mob,asLevel)+(2*super.getX1Level(invoker())))/2;
				int damage = CMLib.dice().roll(1, numDice+10, 5);
				if((msg2.value()>0)||(msg.value()>0))
					damage = (int)Math.round(CMath.div(damage,2.0));
				CMLib.combat().postDamage(mob,target,this,damage,CMMsg.MASK_ALWAYS|CMMsg.TYP_ACID,Weapon.TYPE_MELTING,"The acidic blast <DAMAGES> <T-NAME>!");
				maliciousAffect(mob,target,asLevel,3,-1);
			}
		}
		else
			return maliciousFizzle(mob,target,"<S-NAME> point(s) and conjur(s) at <T-NAMESELF>, but nothing more happens.");

		return success;
	}
}
