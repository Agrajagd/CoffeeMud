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
@SuppressWarnings("rawtypes")
public class Spell_WallOfIce extends Spell
{
	@Override public String ID() { return "Spell_WallOfIce"; }
	@Override public String name(){return "Wall of Ice";}
	@Override public String displayText(){return "(Wall of Ice)";}
	@Override public int maxRange(){return adjustedMaxInvokerRange(10);}
	@Override public int minRange(){return 1;}
	@Override public int abstractQuality(){ return Ability.QUALITY_OK_SELF;}
	@Override public int enchantQuality(){return Ability.QUALITY_INDIFFERENT;}
	@Override protected int canAffectCode(){return CAN_ITEMS;}
	@Override protected int canTargetCode(){return 0;}
	@Override public int classificationCode(){return Ability.ACODE_SPELL|Ability.DOMAIN_CONJURATION;}

	protected int amountRemaining=0;
	protected Item theWall=null;
	protected String deathNotice="";

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if((affected==null)||(!(affected instanceof Item)))
			return true;

		final MOB mob=msg.source();

		if((invoker!=null)
		&&(mob.isInCombat())
		&&(mob.getVictim()==invoker)
		&&(mob.rangeToTarget()==1))
		{
			if(msg.sourceMinor()==CMMsg.TYP_ADVANCE)
			{
				Item w=mob.fetchWieldedItem();
				if(w==null) w=mob.myNaturalWeapon();
				if(w==null) return false;
				final Room room=mob.location();
				final CMMsg msg2=CMClass.getMsg(mob,null,w,CMMsg.MSG_WEAPONATTACK,_("^F^<FIGHT^><S-NAME> hack(s) at the wall of ice with <O-NAME>.^</FIGHT^>^?"));
				CMLib.color().fixSourceFightColor(msg2);
				if(mob.location().okMessage(mob,msg2))
				{
					mob.location().send(mob,msg2);
					amountRemaining-=mob.phyStats().damage();
					if(amountRemaining<0)
					{
						deathNotice="The wall of ice shatters!!!";
						for(int i=0;i<room.numInhabitants();i++)
						{
							final MOB M=room.fetchInhabitant(i);
							if((M.isInCombat())
							&&(M.getVictim()==invoker)
							&&(M.rangeToTarget()>0)
							&&(M.rangeToTarget()<3)
							&&(!M.amDead()))
								CMLib.combat().postDamage(invoker,M,this,CMLib.dice().roll((M.phyStats().level()+getXLEVELLevel(invoker()))/2,6,0),CMMsg.MASK_ALWAYS|CMMsg.TYP_COLD,Weapon.TYPE_PIERCING,"A shard of ice <DAMAGE> <T-NAME>!");
						}
						((Item)affected).destroy();
					}
				}
				return false;
			}
		}
		return super.okMessage(myHost,msg);
	}

	@Override
	public void unInvoke()
	{
		super.unInvoke();
		if(canBeUninvoked())
		{
			if((theWall!=null)
			&&(theWall.owner()!=null)
			&&(theWall.owner() instanceof Room)
			&&(((Room)theWall.owner()).isContent(theWall)))
			{
				final MOB actorM=(invoker!=null)? invoker : CMLib.map().deity();
				((Room)theWall.owner()).show(actorM,null,CMMsg.MSG_OK_VISUAL,deathNotice);
				final Item wall=theWall;
				theWall=null;
				wall.destroy();
			}
		}
	}

	@Override
	public boolean tick(Tickable ticking, int tickID)
	{
		if(tickID==Tickable.TICKID_MOB)
		{
			if((invoker!=null)
			   &&(theWall!=null)
			   &&(invoker.location()!=null)
			   &&(!invoker.location().isContent(theWall)))
				unInvoke();
		}
		return super.tick(ticking,tickID);
	}


	@Override
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		if((!mob.isInCombat())||(mob.rangeToTarget()<1))
		{
			mob.tell(_("You really should be in ranged combat to cast this."));
			return false;
		}
		for(int i=0;i<mob.location().numItems();i++)
		{
			final Item I=mob.location().getItem(i);
			if((I!=null)&&(I.fetchEffect(ID())!=null))
			{
				mob.tell(_("There is already a wall of ice here."));
				return false;
			}
		}

		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final Physical target = mob.location();


		final boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.

			final CMMsg msg = CMClass.getMsg(mob, target, this,verbalCastCode(mob,target,auto),auto?_("A mighty wall of ice appears!"):_("^S<S-NAME> conjur(s) up a mighty wall of ice!^?"));
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				amountRemaining=20;
				final Item I=CMClass.getItem("GenItem");
				I.setName("a wall of ice");
				I.setDisplayText("a mighty wall of ice has been erected here");
				I.setDescription("The ice is crystal clear.");
				I.setMaterial(RawMaterial.RESOURCE_GLASS);
				CMLib.flags().setGettable(I,false);
				I.recoverPhyStats();
				mob.location().addItem(I);
				theWall=I;
				deathNotice="The wall of ice melts.";
				beneficialAffect(mob,I,asLevel,0);
			}
		}
		else
			return beneficialWordsFizzle(mob,null,_("<S-NAME> incant(s), but the magic fizzles."));

		// return whether it worked
		return success;
	}
}
