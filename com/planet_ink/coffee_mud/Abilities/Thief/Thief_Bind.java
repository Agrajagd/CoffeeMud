package com.planet_ink.coffee_mud.Abilities.Thief;
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
public class Thief_Bind extends ThiefSkill
{
	public String ID() { return "Thief_Bind"; }
	public String name(){ return "Bind";}
	public String displayText(){ return "(Bound by "+ropeName+")";}
	protected int canAffectCode(){return CAN_MOBS|CAN_ROOMS;}
	protected int canTargetCode(){return CAN_MOBS;}
	public int abstractQuality(){return Ability.QUALITY_MALICIOUS;}
    public int classificationCode(){return Ability.ACODE_THIEF_SKILL|Ability.DOMAIN_BINDING;}
	private static final String[] triggerStrings = {"BIND"};
	public String[] triggerStrings(){return triggerStrings;}
	protected int maxRange=0;
	public int maxRange(){return maxRange;}
	public int minRange(){return 0;}
	public long flags(){return Ability.FLAG_BINDING;}
	public int usageType(){return USAGE_MOVEMENT;}
	public boolean bubbleAffect(){return affected instanceof Room;}

	public int amountRemaining=500;
	public String ropeName="the ropes";

	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		affectableStats.setDisposition(affectableStats.disposition()|EnvStats.IS_BOUND);
	}
	public boolean okMessage(Environmental myHost, CMMsg msg)
	{
		// when this spell is on a MOBs Affected list,
		// it should consistantly prevent the mob
		// from trying to do ANYTHING except sleep
		if((msg.source()==affected)||(affected instanceof Room))
		{
			if(((msg.sourceMinor()==CMMsg.TYP_SIT)||(msg.sourceMinor()==CMMsg.TYP_STAND))
			&&(affected instanceof MOB))
				return true;
			else
			if(((msg.sourceMinor()==CMMsg.TYP_LEAVE)||(msg.sourceMinor()==CMMsg.TYP_ENTER))
			&&(affected instanceof Room))
				return true;
			else
			if((!CMath.bset(msg.sourceMajor(),CMMsg.MASK_ALWAYS))
			&&((CMath.bset(msg.sourceMajor(),CMMsg.MASK_HANDS))
			||(CMath.bset(msg.sourceMajor(),CMMsg.MASK_MOVE))))
			{
				if(canBeUninvoked())
				{
					if(msg.source().location().show(msg.source(),null,CMMsg.MSG_OK_ACTION,"<S-NAME> struggle(s) against "+ropeName.toLowerCase()+" binding <S-HIM-HER>."))
					{
						amountRemaining-=(msg.source().charStats().getStat(CharStats.STAT_STRENGTH)+msg.source().envStats().level());
						if(amountRemaining<0)
							unInvoke();
					}
				}
				else
					msg.source().tell("You are constricted by "+ropeName.toLowerCase()+" and can't move!");
				return false;
			}
		}
		return super.okMessage(myHost,msg);
	}


	public void setAffectedOne(Environmental E)
	{
		if(!(E instanceof Item))
			super.setAffectedOne(E);
		else
			ropeName=E.name();
	}

	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
		{
			super.unInvoke();
			return;
		}
		MOB mob=(MOB)affected;

		super.unInvoke();
		if(canBeUninvoked())
		{
			if(!mob.amDead())
				mob.location().show(mob,null,CMMsg.MSG_NOISYMOVEMENT,"<S-NAME> manage(s) to break <S-HIS-HER> way free of "+ropeName+".");
			CMLib.commands().postStand(mob,true);
		}
	}

	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto, int asLevel)
	{
		if((mob.isInCombat())&&(!auto))
		{
			mob.tell("Not while you are fighting!");
			return false;
		}
	    if((commands.size()>0)&&((String)commands.firstElement()).equalsIgnoreCase("UNTIE"))
	    {
	        commands.removeElementAt(0);
			MOB target=super.getTarget(mob,commands,givenTarget,false,true);
			if(target==null) return false;
			Ability A=target.fetchEffect(ID());
			if(A!=null)
			{
			    if(mob.location().show(mob,target,null,CMMsg.MSG_HANDS,"<S-NAME> attempt(s) to unbind <T-NAMESELF>."))
			    {
				    A.unInvoke();
				    return true;
			    }
		        return false;
			}
		    mob.tell(target.name()+" doesn't appear to be bound with ropes.");
		    return false;
	    }
	            
	            
		MOB target=getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		if((!CMLib.flags().isSleeping(target))&&(CMLib.flags().canMove(target)&&(!auto)))
		{
			mob.tell(target.name()+" doesn't look willing to cooperate.");
			return false;
		}
		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			if(auto) maxRange=10;
			String str=auto?"<T-NAME> become(s) bound by "+ropeName+".":"<S-NAME> bind(s) <T-NAME> with "+ropeName+".";
			CMMsg msg=CMClass.getMsg(mob,target,this,(auto?CMMsg.MASK_ALWAYS:0)|CMMsg.MSG_THIEF_ACT|CMMsg.MASK_SOUND|CMMsg.MASK_MALICIOUS,auto?"":str,str,str);
			if((target.location().okMessage(mob,msg))&&(target.fetchEffect(this.ID())==null))
			{
				target.location().send(mob,msg);
				if(msg.value()<=0)
				{
					if(auto)
					{
						maxRange=0;
						double prof=0.0;
						Ability A=mob.fetchAbility("Specialization_Ranged");
						if(A!=null) prof=CMath.div(A.proficiency(),20);
						amountRemaining=(mob.charStats().getStat(CharStats.STAT_STRENGTH)+mob.envStats().level()+(2*getXLEVELLevel(mob)))*((int)Math.round(5.0+prof));
					}
					else
						amountRemaining=(adjustedLevel(mob,asLevel)+getXLEVELLevel(mob))*25;
					if((target.location()==mob.location())||(auto))
						success=maliciousAffect(mob,target,asLevel,Integer.MAX_VALUE-1000,-1);
				}
				if((mob.getVictim()==target)&&(!auto))
					mob.makePeace();
			}
		}
		else
			return maliciousFizzle(mob,target,"<S-NAME> attempt(s) to bind <T-NAME> and fail(s).");


		// return whether it worked
		return success;
	}
}
