package com.planet_ink.coffee_mud.Abilities.Druid;
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
public class Chant_SummonPlants extends Chant
{
	@Override public String ID() { return "Chant_SummonPlants"; }
	@Override public String name(){ return "Summon Plants";}
	@Override public int classificationCode(){return Ability.ACODE_CHANT|Ability.DOMAIN_PLANTGROWTH;}
	@Override public int abstractQuality(){return Ability.QUALITY_INDIFFERENT;}
	@Override protected int canAffectCode(){return CAN_ITEMS;}
	@Override protected int canTargetCode(){return 0;}
	protected Room PlantsLocation=null;
	protected Item littlePlants=null;
	protected static Hashtable plantBonuses=new Hashtable();

	@Override
	public void unInvoke()
	{
		if(PlantsLocation==null)
			return;
		if(littlePlants==null)
			return;
		if(canBeUninvoked())
			PlantsLocation.showHappens(CMMsg.MSG_OK_VISUAL,_("@x1 wither@x2 away.",littlePlants.name(),(littlePlants.name().startsWith("s")?"":"s")));
		super.unInvoke();
		if(canBeUninvoked())
		{
			final Item plants=littlePlants; // protects against uninvoke loops!
			littlePlants=null;
			plants.destroy();
			PlantsLocation.recoverRoomStats();
			PlantsLocation=null;
		}
	}

	@Override
	public String text()
	{
		if((miscText.length()==0)
		&&(invoker()!=null))
			miscText=invoker().Name();
		return super.text();
	}

	@Override
	public void executeMsg(final Environmental myHost, final CMMsg msg)
	{
		if((msg.amITarget(littlePlants))
		&&((msg.targetMinor()==CMMsg.TYP_GET)||(msg.targetMinor()==CMMsg.TYP_PUSH)||(msg.targetMinor()==CMMsg.TYP_PULL)))
			msg.addTrailerMsg(CMClass.getMsg(msg.source(),littlePlants,null,CMMsg.MSG_OK_VISUAL,CMMsg.MASK_ALWAYS|CMMsg.MSG_DEATH,CMMsg.NO_EFFECT,null));
	}

	public static Item buildPlant(MOB mob, Room room)
	{
		final Item newItem=CMClass.getItem("GenItem");
		newItem.setMaterial(RawMaterial.RESOURCE_GREENS);
		switch(CMLib.dice().roll(1,5,0))
		{
		case 1:
			newItem.setName("some happy flowers");
			newItem.setDisplayText("some happy flowers are growing here.");
			newItem.setDescription("Happy flowers with little red and yellow blooms.");
			break;
		case 2:
			newItem.setName("some happy weeds");
			newItem.setDisplayText("some happy weeds are growing here.");
			newItem.setDescription("Long stalked little plants with tiny bulbs on top.");
			break;
		case 3:
			newItem.setName("a pretty fern");
			newItem.setDisplayText("a pretty fern is growing here.");
			newItem.setDescription("Like a tiny bush, this dark green plant is lovely.");
			break;
		case 4:
			newItem.setName("a patch of sunflowers");
			newItem.setDisplayText("a patch of sunflowers is growing here.");
			newItem.setDescription("Happy flowers with little yellow blooms.");
			break;
		case 5:
			newItem.setName("a patch of bluebonnets");
			newItem.setDisplayText("a patch of bluebonnets is growing here.");
			newItem.setDescription("Happy flowers with little blue and purple blooms.");
			break;
		}
		final Chant_SummonPlants newChant=new Chant_SummonPlants();
		newItem.basePhyStats().setLevel(10+(10*newChant.getX1Level(mob)));
		newItem.basePhyStats().setWeight(1);
		newItem.setSecretIdentity(mob.Name());
		newItem.setMiscText(newItem.text());
		room.addItem(newItem);
		newItem.setExpirationDate(0);
		room.showHappens(CMMsg.MSG_OK_ACTION,CMLib.lang()._("Suddenly, @x1 sprout(s) up here.",newItem.name()));
		newChant.PlantsLocation=room;
		newChant.littlePlants=newItem;
		if(CMLib.law().doesOwnThisProperty(mob,room))
		{
			newChant.setInvoker(mob);
			newChant.setMiscText(mob.Name());
			newItem.addNonUninvokableEffect(newChant);
		}
		else
			newChant.beneficialAffect(mob,newItem,0,(newChant.adjustedLevel(mob,0)*240)+450);
		room.recoverPhyStats();
		return newItem;
	}

	public Item buildMyThing(MOB mob, Room room)
	{
		final Area A=room.getArea();
		final boolean bonusWorthy=(Druid_MyPlants.myPlant(room,mob,0)==null);
		final Vector V=Druid_MyPlants.myAreaPlantRooms(mob,room.getArea());
		int pct=0;
		if(A.getAreaIStats()[Area.Stats.VISITABLE_ROOMS.ordinal()]>10)
			pct=(int)Math.round(100.0*CMath.div(V.size(),A.getAreaIStats()[Area.Stats.VISITABLE_ROOMS.ordinal()]));
		final Item I=buildMyPlant(mob,room);
		if((I!=null)
		&&((mob.charStats().getCurrentClass().baseClass().equalsIgnoreCase("Druid"))||(CMSecurity.isASysOp(mob))))
		{
			if(!CMLib.law().isACity(A))
			{
				if(pct>0)
				{
					final int newPct=(int)Math.round(100.0*CMath.div(V.size(),A.getAreaIStats()[Area.Stats.VISITABLE_ROOMS.ordinal()]));
					if((newPct>=50)&&(A.fetchEffect("Chant_DruidicConnection")==null))
					{
						final Ability A2=CMClass.getAbility("Chant_DruidicConnection");
						if(A2!=null) A2.invoke(mob,A,true,0);
					}
				}
			}
			else
			if((bonusWorthy)&&(!mob.isMonster()))
			{
				long[] num=(long[])plantBonuses.get(mob.Name()+"/"+room.getArea().Name());
				if((num==null)||(System.currentTimeMillis()-num[1]>(room.getArea().getTimeObj().getDaysInMonth()*room.getArea().getTimeObj().getHoursInDay()*CMProps.getMillisPerMudHour())))
				{
					num=new long[2];
					plantBonuses.remove(mob.Name()+"/"+room.getArea().Name());
					plantBonuses.put(mob.Name()+"/"+room.getArea().Name(),num);
					num[1]=System.currentTimeMillis();
				}
				if(V.size()>=num[0])
				{
					num[0]++;
					if(num[0]<19)
					{
						mob.tell(_("You have made this city greener."));
						CMLib.leveler().postExperience(mob,null,null,(int)num[0],false);
					}
				}
			}
		}
		return I;
	}

	protected Item buildMyPlant(MOB mob, Room room){ return buildPlant(mob,room);}

	public boolean rightPlace(MOB mob,boolean auto)
	{
		if((!auto)&&(mob.location().domainType()&Room.INDOORS)>0)
		{
			mob.tell(_("You must be outdoors for this chant to work."));
			return false;
		}

		if((mob.location().domainType()==Room.DOMAIN_OUTDOORS_CITY)
		   ||(mob.location().domainType()==Room.DOMAIN_OUTDOORS_SPACEPORT)
		   ||(mob.location().domainType()==Room.DOMAIN_OUTDOORS_UNDERWATER)
		   ||(mob.location().domainType()==Room.DOMAIN_OUTDOORS_AIR)
		   ||(mob.location().domainType()==Room.DOMAIN_OUTDOORS_WATERSURFACE))
		{
			mob.tell(_("This magic will not work here."));
			return false;
		}
		return true;
	}

	@Override
	public int castingQuality(MOB mob, Physical target)
	{
		if(mob!=null)
		{
			if(!rightPlace(mob,false))
				return Ability.QUALITY_INDIFFERENT;
			final Item myPlant=Druid_MyPlants.myPlant(mob.location(),mob,0);
			if(myPlant==null)
				return super.castingQuality(mob, target,Ability.QUALITY_BENEFICIAL_SELF);
		}
		return super.castingQuality(mob,target);
	}

	@Override
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		if(!rightPlace(mob,auto)) return false;

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		// now see if it worked
		final boolean success=proficiencyCheck(mob,0,auto);
		if(success)
		{
			final CMMsg msg=CMClass.getMsg(mob,null,this,verbalCastCode(mob,null,auto),auto?"":"^S<S-NAME> chant(s) to the ground.^?");
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				buildMyThing(mob,mob.location());
			}
		}
		else
			return beneficialWordsFizzle(mob,null,_("<S-NAME> chant(s) to the ground, but nothing happens."));

		// return whether it worked
		return success;
	}
}
