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
public class Spell_AweOther extends Spell
{
	@Override public String ID() { return "Spell_AweOther"; }
	@Override public String name(){return "Awe Other";}
	@Override public String displayText(){return "(Awe of "+text()+")";}
	@Override protected int canAffectCode(){return CAN_MOBS;}
	@Override public int abstractQuality(){return Ability.QUALITY_MALICIOUS;}
	@Override public int classificationCode(){	return Ability.ACODE_SPELL|Ability.DOMAIN_ENCHANTMENT;}

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if(((msg.targetMajor()&CMMsg.MASK_MALICIOUS)>0)
		&&(!CMath.bset(msg.sourceMajor(),CMMsg.MASK_ALWAYS))
		&&(msg.target()!=null)
		&&(msg.target().Name().equalsIgnoreCase(text())))
		{
			final MOB target=(MOB)msg.target();
			if((!target.isInCombat())
			&&(msg.source().getVictim()!=target)
			&&(msg.source().location()==target.location()))
			{
				msg.source().tell(_("You are too much in awe of @x1",target.name(msg.source())));
				if(target.getVictim()==msg.source())
				{
					target.makePeace();
					target.setVictim(null);
				}
				return false;
			}
		}
		return super.okMessage(myHost,msg);
	}

	@Override
	public int castingQuality(MOB mob, Physical target)
	{
		if(mob!=null)
		{
			if(target instanceof MOB)
			{
				if(((MOB)target).isInCombat())
					return Ability.QUALITY_INDIFFERENT;
			}
		}
		return super.castingQuality(mob,target);
	}

	@Override
	public void unInvoke()
	{
		// undo the affects of this spell
		if(!(affected instanceof MOB))
			return;
		final MOB mob=(MOB)affected;
		super.unInvoke();

		if(canBeUninvoked())
			if((mob.location()!=null)&&(!mob.amDead()))
				mob.location().show(mob,null,CMMsg.MSG_OK_VISUAL,_("<S-NAME> seem(s) less in awe of @x1.",text()));
	}



	@Override
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		if(commands.size()<2)
		{
			mob.tell(_("Invoke awe on whom and of whom?"));
			return false;
		}
		final String aweWhom=CMParms.combine(commands,1);
		final MOB target=getTarget(mob,new XVector(commands.firstElement()),givenTarget);
		if(target==null) return false;
		Room R=CMLib.map().roomLocation(target);
		if(R==null) R=mob.location();

		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			invoker=mob;
			final CMMsg msg=CMClass.getMsg(mob,target,this,verbalCastCode(mob,target,auto),auto?"":"^S<S-NAME> invoke(s) a spell on <T-NAMESELF>.^?");
			if(R.okMessage(mob,msg))
			{
				R.send(mob,msg);
				if(maliciousAffect(mob,target,asLevel,0,-1))
				{
					final Ability A=target.fetchEffect(ID());
					if(A!=null)
					{
						A.setMiscText(CMStrings.capitalizeAndLower(aweWhom));
						R.show(target,null,CMMsg.MSG_OK_VISUAL,_("<S-NAME> gain(s) a new awe of @x1!",CMStrings.capitalizeAndLower(aweWhom)));
					}
				}
			}
		}
		else
			return beneficialWordsFizzle(mob,target,_("<S-NAME> attempt(s) to invoke a spell on <T-NAMESELF>, but fail(s) miserably."));

		// return whether it worked
		return success;
	}
}
