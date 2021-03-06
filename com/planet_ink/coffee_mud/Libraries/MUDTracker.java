package com.planet_ink.coffee_mud.Libraries;
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

import com.planet_ink.coffee_mud.Libraries.interfaces.*;


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
public class MUDTracker extends StdLibrary implements TrackingLibrary
{
	@Override public String ID(){return "MUDTracker";}
	protected Hashtable<Integer,Vector<String>> directionCommandSets=new Hashtable<Integer,Vector<String>>();
	protected Hashtable<Integer,Vector<String>> openCommandSets=new Hashtable<Integer,Vector<String>>();
	protected Hashtable<Integer,Vector<String>> closeCommandSets=new Hashtable<Integer,Vector<String>>();
	protected Hashtable<TrackingFlags,RFilters> trackingFilters=new Hashtable<TrackingFlags,RFilters>();
	protected static final TrackingFlags		EMPTY_FLAGS=new TrackingFlags();
	protected static final RFilters				EMPTY_FILTERS=new RFilters();


	protected Vector<String> getDirectionCommandSet(int direction)
	{
		final Integer dir=Integer.valueOf(direction);
		if(!directionCommandSets.containsKey(dir))
		{
			final Vector<String> V=new ReadOnlyVector<String>(Directions.getDirectionName(direction));
			directionCommandSets.put(dir, V);
		}
		return directionCommandSets.get(dir);
	}

	protected Vector<String> getOpenCommandSet(int direction)
	{
		final Integer dir=Integer.valueOf(direction);
		if(!directionCommandSets.containsKey(dir))
		{
			final Vector<String> V=new ReadOnlyVector<String>(CMParms.parse("OPEN "+Directions.getDirectionName(direction)));
			directionCommandSets.put(dir, V);
		}
		return directionCommandSets.get(dir);
	}

	protected Vector<String> getCloseCommandSet(int direction)
	{
		final Integer dir=Integer.valueOf(direction);
		if(!directionCommandSets.containsKey(dir))
		{
			final Vector<String> V=new ReadOnlyVector<String>(CMParms.parse("CLOSE "+Directions.getDirectionName(direction)));
			directionCommandSets.put(dir, V);
		}
		return directionCommandSets.get(dir);
	}

	@Override
	public List<Room> findBastardTheBestWay(Room location,
											Room destRoom,
											TrackingFlags flags,
											int maxRadius)
	{
		return findBastardTheBestWay(location,destRoom,flags,maxRadius,null);
	}

	public List<Room> findBastardTheBestWay(Room location,
											Room destRoom,
											TrackingFlags flags,
											int maxRadius,
											List<Room> radiant)
	{
		if((radiant==null)||(radiant.size()==0))
		{
			radiant=new Vector<Room>();
			getRadiantRooms(location,radiant,flags,destRoom,maxRadius,null);
			if(!radiant.contains(location))
				radiant.add(0,location);
		}
		else
		{
			final List<Room> radiant2=new Vector<Room>(radiant.size());
			int r=0;
			boolean foundLocation=false;
			Room O;
			for(;r<radiant.size();r++)
			{
				O=radiant.get(r);
				radiant2.add(O);
				if((!foundLocation)&&(O==location))
					foundLocation=true;
				if(O==destRoom) break;
			}
			if(!foundLocation)
			{
				radiant.add(0,location);
				radiant2.add(0,location);
			}
			if(r>=radiant.size()) return null;
			radiant=radiant2;
		}
		if((radiant.size()>0)&&(destRoom==radiant.get(radiant.size()-1)))
		{
			List<Room> thisTrail=new Vector<Room>();
			final HashSet<Room> tried=new HashSet<Room>();
			thisTrail.add(destRoom);
			tried.add(destRoom);
			Room R=null;
			int index=radiant.size()-2;
			if(destRoom!=location)
			while(index>=0)
			{
				int best=-1;
				for(int i=index;i>=0;i--)
				{
					R=radiant.get(i);
					for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
					{
						if((R.getRoomInDir(d)==thisTrail.get(thisTrail.size()-1))
						&&(R.getExitInDir(d)!=null)
						&&(!tried.contains(R)))
							best=i;
					}
				}
				if(best>=0)
				{
					R=radiant.get(best);
					thisTrail.add(R);
					tried.add(R);
					if(R==location) break;
					index=best-1;
				}
				else
				{
					thisTrail.clear();
					thisTrail=null;
					break;
				}
			}
			return thisTrail;
		}
		return null;
	}

	@Override
	public void markToWanderHomeLater(MOB M)
	{
		final Ability A=CMClass.getAbility("WanderHomeLater");
		if(A!=null)
		{
			M.addEffect(A);
			A.makeLongLasting();
		}
	}

	@Override
	public List<Room> findBastardTheBestWay(Room location,
											List<Room> destRooms,
											TrackingFlags flags,
											int maxRadius)
	{

		List<Room> finalTrail=null;
		Room destRoom=null;
		int pick=0;
		List<Room> radiant=null;
		if(destRooms.size()>1)
		{
			radiant=new Vector<Room>();
			getRadiantRooms(location,radiant,flags,null,maxRadius,null);
		}
		for(int i=0;(i<5)&&(destRooms.size()>0);i++)
		{
			pick=CMLib.dice().roll(1,destRooms.size(),-1);
			destRoom=destRooms.get(pick);
			destRooms.remove(pick);
			final List<Room> thisTrail=findBastardTheBestWay(location,destRoom,flags,maxRadius,radiant);
			if((thisTrail!=null)
			&&((finalTrail==null)||(thisTrail.size()<finalTrail.size())))
				finalTrail=thisTrail;
		}
		if(finalTrail==null)
		for(int r=0;r<destRooms.size();r++)
		{
			destRoom=destRooms.get(r);
			final List<Room> thisTrail=findBastardTheBestWay(location,destRoom,flags,maxRadius);
			if((thisTrail!=null)
			&&((finalTrail==null)||(thisTrail.size()<finalTrail.size())))
				finalTrail=thisTrail;
		}
		return finalTrail;
	}

	@Override
	public int trackNextDirectionFromHere(List<Room> theTrail,
										  Room location,
										  boolean openOnly)
	{
		if((theTrail==null)||(location==null))
			return -1;
		if(location==theTrail.get(0))
			return 999;
		int locationLocation=theTrail.indexOf(location);
		if(locationLocation<0) locationLocation=Integer.MAX_VALUE;
		Room R=null;
		Exit E=null;
		int x=0;
		int winningDirection=-1;
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
		{
			R=location.getRoomInDir(d);
			E=location.getExitInDir(d);
			if((R!=null)
			&&(E!=null)
			&&((!openOnly)||(E.isOpen())))
			{
				x=theTrail.indexOf(R);
				if((x>=0)&&(x<locationLocation))
				{
					locationLocation=x;
					winningDirection=d;
				}
			}
		}
		return winningDirection;
	}

	@Override
	public int radiatesFromDir(Room room, List<Room> rooms)
	{
		for(final Room R : rooms)
		{
			if(R==room) return -1;
			for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
				if(R.getRoomInDir(d)==room)
					return Directions.getOpDirectionCode(d);
		}
		return -1;
	}

	@Override
	public List<Room> getRadiantRooms(final Room room, final TrackingFlags flags, final int maxDepth)
	{
		final List<Room> V=new Vector<Room>();
		getRadiantRooms(room,V,flags,null,maxDepth,null);
		return V;
	}
	@Override
	public List<Room> getRadiantRooms(final Room room, final RFilters filters, final int maxDepth)
	{
		final List<Room> V=new Vector<Room>();
		getRadiantRooms(room,V,filters,null,maxDepth,null);
		return V;
	}

	@Override
	public void getRadiantRooms(final Room room, List<Room> rooms, TrackingFlags flags, final Room radiateTo, final int maxDepth, final Set<Room> ignoreRooms)
	{
		if(flags == null)
			flags = EMPTY_FLAGS;
		RFilters filters=trackingFilters.get(flags);
		if(filters==null)
		{
			if(flags.size()==0)
				filters=EMPTY_FILTERS;
			else
			{
				filters=new RFilters();
				for(final TrackingFlag flag : flags)
					filters.plus(flag.myFilter);
			}
			trackingFilters.put(flags, filters);
		}
		getRadiantRooms(room, rooms, filters, radiateTo, maxDepth, ignoreRooms);
	}

	@Override
	public void getRadiantRooms(final Room room, List<Room> rooms, final RFilters filters, final Room radiateTo, final int maxDepth, final Set<Room> ignoreRooms)
	{
		int depth=0;
		if(room==null) return;
		if(rooms.contains(room)) return;
		final HashSet<Room> H=new HashSet<Room>(1000);
		rooms.add(room);
		if(rooms instanceof Vector<?>)
			((Vector<Room>)rooms).ensureCapacity(200);
		if(ignoreRooms != null)
			H.addAll(ignoreRooms);
		for(int r=0;r<rooms.size();r++)
			H.add(rooms.get(r));
		int min=0;
		int size=rooms.size();
		Room R1=null;
		Room R=null;
		Exit E=null;

		int r=0;
		int d=0;
		while(depth<maxDepth)
		{
			for(r=min;r<size;r++)
			{
				R1=rooms.get(r);
				for(d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
				{
					R=R1.getRoomInDir(d);
					E=R1.getExitInDir(d);

					if((R==null)||(E==null)
					||(H.contains(R))
					||(filters.isFilteredOut(R, E, d)))
						continue;
					rooms.add(R);
					H.add(R);
					if(R==radiateTo) // R can't be null here, so if they are equal, time to go!
						return;
				}
			}
			min=size;
			size=rooms.size();
			if(min==size) return;
			depth++;
		}
	}

	@Override
	public void stopTracking(MOB mob)
	{
		final List<Ability> V=CMLib.flags().flaggedAffects(mob,Ability.FLAG_TRACKING);
		for(final Ability A : V)
		{ A.unInvoke(); mob.delEffect(A);}
	}

	public boolean isAnAdminHere(Room R)
	{
		final Set<MOB> mobsThere=CMLib.players().getPlayersHere(R);
		if(mobsThere.size()>0)
		{
			for(final MOB inhab : mobsThere)
			{
				if((inhab.session()!=null)
				&&(CMSecurity.isAllowed(inhab,R,CMSecurity.SecFlag.CMDMOBS)||CMSecurity.isAllowed(inhab,R,CMSecurity.SecFlag.CMDROOMS))
				&&(CMLib.flags().isInTheGame(inhab, true))
				&&(CMath.bset(inhab.getBitmap(), MOB.ATT_SYSOPMSGS)))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean beMobile(final MOB mob,
							final boolean dooropen,
							final boolean wander,
							final boolean roomprefer,
							final boolean roomobject,
							final int[] status,
							final List<Room> rooms)
	{
		return beMobile(mob,dooropen,wander,roomprefer,roomobject,true,status,rooms);

	}
	private boolean beMobile(final MOB mob,
							 boolean dooropen,
							 final boolean wander,
							 final boolean roomprefer,
							 final boolean roomobject,
							 final boolean sneakIfAble,
							 final int[] status,
							 final List<Room> rooms)
	{
		if(status!=null)status[0]=Tickable.STATUS_MISC7+0;

		// ridden and following things aren't mobile!
		if(((mob instanceof Rideable)&&(((Rideable)mob).numRiders()>0))
		||((mob.amFollowing()!=null)&&(mob.location()==mob.amFollowing().location())))
		{
			if(status!=null)status[0]=Tickable.STATUS_NOT;
			return false;
		}

		Room oldRoom=mob.location();

		if(isAnAdminHere(oldRoom))
		{
			if(status!=null)status[0]=Tickable.STATUS_NOT;
			return false;
		}

		if(oldRoom instanceof GridLocale)
		{
			final Room R=((GridLocale)oldRoom).getRandomGridChild();
			if(R!=null) R.bringMobHere(mob,true);
			oldRoom=mob.location();
		}

		if(status!=null)status[0]=Tickable.STATUS_MISC7+3;
		int tries=0;
		int direction=-1;
		while(((tries++)<10)&&(direction<0))
		{
			direction=CMLib.dice().roll(1,Directions.NUM_DIRECTIONS(),-1);
			final Room nextRoom=oldRoom.getRoomInDir(direction);
			final Exit nextExit=oldRoom.getExitInDir(direction);
			if((nextRoom!=null)&&(nextExit!=null))
			{
				if(isAnAdminHere(nextRoom))
				{
					direction=-1;
					continue;
				}

				final Exit opExit=nextRoom.getExitInDir(Directions.getOpDirectionCode(direction));
				if(CMLib.flags().isTrapped(nextExit)
				||(CMLib.flags().isHidden(nextExit)&&(!CMLib.flags().canSeeHidden(mob)))
				||(CMLib.flags().isInvisible(nextExit)&&(!CMLib.flags().canSeeInvisible(mob))))
					direction=-1;
				else
				if((opExit!=null)&&(CMLib.flags().isTrapped(opExit)))
					direction=-1;
				else
				if((oldRoom.domainType()!=nextRoom.domainType())
				&&(!CMLib.flags().isInFlight(mob))
				&&((nextRoom.domainType()==Room.DOMAIN_INDOORS_AIR)
				||(nextRoom.domainType()==Room.DOMAIN_OUTDOORS_AIR)))
					direction=-1;
				else
				if((oldRoom.domainType()!=nextRoom.domainType())
				&&(!CMLib.flags().isSwimming(mob))
				&&((nextRoom.domainType()==Room.DOMAIN_INDOORS_UNDERWATER)
				||(nextRoom.domainType()==Room.DOMAIN_OUTDOORS_UNDERWATER)))
					direction=-1;
				else
				if((!wander)&&(!oldRoom.getArea().Name().equals(nextRoom.getArea().Name())))
					direction=-1;
				else
				if((roomobject)&&(rooms!=null)&&(rooms.contains(nextRoom)))
					direction=-1;
				else
				if((roomprefer)&&(rooms!=null)&&(!rooms.contains(nextRoom)))
					direction=-1;
				else
					break;
			}
			else
				direction=-1;
		}

		if(status!=null)status[0]=Tickable.STATUS_MISC7+10;

		if(direction<0)
		{
			if(status!=null)status[0]=Tickable.STATUS_NOT;
			return false;
		}

		final Room nextRoom=oldRoom.getRoomInDir(direction);
		final Exit nextExit=oldRoom.getExitInDir(direction);
		final int opDirection=Directions.getOpDirectionCode(direction);

		if((nextRoom==null)||(nextExit==null))
		{
			if(status!=null)status[0]=Tickable.STATUS_NOT;
			return false;
		}

		if(dooropen)
		{
			final LandTitle landTitle=CMLib.law().getLandTitle(nextRoom);
			if((landTitle!=null)&&(landTitle.getOwnerName().length()>0))
				dooropen=false;
		}

		boolean reclose=false;
		boolean relock=false;
		// handle doors!
		if(nextExit.hasADoor()&&(!nextExit.isOpen())&&(dooropen))
		{
			if((nextExit.hasALock())&&(nextExit.isLocked()))
			{
				CMMsg msg=CMClass.getMsg(mob,nextExit,null,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_OK_VISUAL,null);
				if(oldRoom.okMessage(mob,msg))
				{
					relock=true;
					msg=CMClass.getMsg(mob,nextExit,null,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_UNLOCK,CMMsg.MSG_OK_VISUAL,_("<S-NAME> unlock(s) <T-NAMESELF>."));
					if(oldRoom.okMessage(mob,msg))
						CMLib.utensils().roomAffectFully(msg,oldRoom,direction);
				}
			}
			if(!nextExit.isOpen())
			{
				mob.doCommand(getOpenCommandSet(direction),Command.METAFLAG_FORCED);
				if(nextExit.isOpen())
					reclose=true;
			}
		}
		if(!nextExit.isOpen())
		{
			if(status!=null)status[0]=Tickable.STATUS_NOT;
			return false;
		}

		if(((nextRoom.domainType()==Room.DOMAIN_OUTDOORS_WATERSURFACE)
		||(nextRoom.domainType()==Room.DOMAIN_OUTDOORS_WATERSURFACE))
		   &&(!CMLib.flags().isWaterWorthy(mob))
		   &&(!CMLib.flags().isInFlight(mob))
		   &&(mob.fetchAbility("Skill_Swim")!=null))
		{
			final Ability A=mob.fetchAbility("Skill_Swim");
			final Vector<String> V=getDirectionCommandSet(direction);
			if(A.proficiency()<50)	A.setProficiency(CMLib.dice().roll(1,50,A.adjustedLevel(mob,0)*15));
			final CharState oldState=(CharState)mob.curState().copyOf();
			A.invoke(mob,V,null,false,0);
			mob.curState().setMana(oldState.getMana());
			mob.curState().setMovement(oldState.getMovement());
		}
		else
		if(((nextRoom.ID().indexOf("Surface")>0)||(CMLib.flags().isClimbing(nextExit))||(CMLib.flags().isClimbing(nextRoom)))
		&&(!CMLib.flags().isClimbing(mob))
		&&(!CMLib.flags().isInFlight(mob))
		&&((mob.fetchAbility("Skill_Climb")!=null)||(mob.fetchAbility("Power_SuperClimb")!=null)))
		{
			Ability A=mob.fetchAbility("Skill_Climb");
			if(A==null )A=mob.fetchAbility("Power_SuperClimb");
			final Vector<String> V=getDirectionCommandSet(direction);
			if(A.proficiency()<50)	A.setProficiency(CMLib.dice().roll(1,50,A.adjustedLevel(mob,0)*15));
			final CharState oldState=(CharState)mob.curState().copyOf();
			A.invoke(mob,V,null,false,0);
			mob.curState().setMana(oldState.getMana());
			mob.curState().setMovement(oldState.getMovement());
		}
		else
		if((mob.fetchAbility("Thief_Sneak")!=null)&&(sneakIfAble))
		{
			final Ability A=mob.fetchAbility("Thief_Sneak");
			final Vector<String> V=getDirectionCommandSet(direction);
			if(A.proficiency()<50)
			{
				A.setProficiency(CMLib.dice().roll(1,50,A.adjustedLevel(mob,0)*15));
				final Ability A2=mob.fetchAbility("Thief_Hide");
				if(A2!=null)
					A2.setProficiency(CMLib.dice().roll(1,50,A.adjustedLevel(mob,0)*15));
			}
			final int oldMana=mob.curState().getMana();
			final int oldMove=mob.curState().getMovement();
			A.invoke(mob,V,null,false,0);
			mob.curState().setMana(oldMana);
			mob.curState().setMovement(oldMove);
		}
		else
		{
			walk(mob,direction,false,false);
		}
		if(status!=null)status[0]=Tickable.STATUS_MISC7+21;

		if((reclose)&&(mob.location()==nextRoom)&&(dooropen))
		{
			final Exit opExit=nextRoom.getExitInDir(opDirection);
			if((opExit!=null)
			&&(opExit.hasADoor())
			&&(opExit.isOpen()))
			{
				mob.doCommand(getCloseCommandSet(opDirection),Command.METAFLAG_FORCED);
				if((opExit.hasALock())&&(relock))
				{
					CMMsg msg=CMClass.getMsg(mob,opExit,null,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_OK_VISUAL,null);
					if(nextRoom.okMessage(mob,msg))
					{
						msg=CMClass.getMsg(mob,opExit,null,CMMsg.MSG_OK_VISUAL,CMMsg.MSG_LOCK,CMMsg.MSG_OK_VISUAL,_("<S-NAME> lock(s) <T-NAMESELF>."));
						if(nextRoom.okMessage(mob,msg))
							CMLib.utensils().roomAffectFully(msg,nextRoom,opDirection);
					}
				}
			}
		}
		if(status!=null)status[0]=Tickable.STATUS_NOT;
		return mob.location()!=oldRoom;
	}

	@Override
	public void wanderAway(MOB M, boolean mindPCs, boolean andGoHome)
	{
		if(M==null) return;
		final Room R=M.location();
		if(R==null) return;
		int tries=0;
		while((M.location()==R)&&((++tries)<100)&&((!mindPCs)||(R.numPCInhabitants()>0)))
			beMobile(M,true,true,false,false,false,null,null);
		if((M.getStartRoom()!=null)&&(andGoHome))
			M.getStartRoom().bringMobHere(M,true);
	}

	@Override
	public void wanderFromTo(MOB M, Room toHere, boolean mindPCs)
	{
		if(M==null) return;
		if((M.location()!=null)&&(M.location().isInhabitant(M)))
			wanderAway(M,mindPCs,false);
		wanderIn(M,toHere);
	}

	@Override
	public void wanderIn(MOB M, Room toHere)
	{
		if(toHere==null) return;
		if(M==null) return;
		int tries=0;
		int dir=-1;
		while((dir<0)&&((++tries)<100))
		{
			dir=CMLib.dice().roll(1,Directions.NUM_DIRECTIONS(),-1);
			final Room R=toHere.getRoomInDir(dir);
			if(R!=null)
			{
				if(((R.domainType()==Room.DOMAIN_INDOORS_AIR)&&(!CMLib.flags().isFlying(M)))
				||((R.domainType()==Room.DOMAIN_OUTDOORS_AIR)&&(!CMLib.flags().isFlying(M)))
				||((R.domainType()==Room.DOMAIN_INDOORS_UNDERWATER)&&(!CMLib.flags().isSwimming(M)))
				||((R.domainType()==Room.DOMAIN_OUTDOORS_UNDERWATER)&&(!CMLib.flags().isSwimming(M)))
				||((R.domainType()==Room.DOMAIN_OUTDOORS_WATERSURFACE)&&(!CMLib.flags().isWaterWorthy(M)))
				||((R.domainType()==Room.DOMAIN_OUTDOORS_WATERSURFACE)&&(!CMLib.flags().isWaterWorthy(M))))
					dir=-1;
			}
			else
				dir=-1;
		}
		if(dir<0)
			toHere.show(M,null,null,CMMsg.MSG_OK_ACTION,_("<S-NAME> wanders in."));
		else
		{
			final String inDir=((toHere instanceof SpaceShip)||(toHere.getArea() instanceof SpaceShip))?
					Directions.getShipDirectionName(dir):Directions.getDirectionName(dir);
			toHere.show(M,null,null,CMMsg.MSG_OK_ACTION,_("<S-NAME> wanders in from @x1.",inDir));
		}
		toHere.bringMobHere(M,true);
	}

	public void ridersBehind(List<Rider> riders, Room sourceRoom, Room destRoom, int directionCode, boolean flee, boolean running)
	{
		if(riders!=null)
		for(final Rider rider : riders)
		{
			if(rider instanceof MOB)
			{
				final MOB rMOB=(MOB)rider;

				if((rMOB.location()==sourceRoom)||(rMOB.location()==destRoom))
				{
					boolean fallOff=false;
					if(rMOB.location()==sourceRoom)
					{
						if(rMOB.riding()!=null)
						{
							final String inDir=((sourceRoom instanceof SpaceShip)||(sourceRoom.getArea() instanceof SpaceShip))?
									Directions.getShipDirectionName(directionCode):Directions.getDirectionName(directionCode);
							rMOB.tell(_("You ride @x1 @x2.",rMOB.riding().name(),inDir));
						}
						if(!move(rMOB,directionCode,flee,false,true,false,running))
							fallOff=true;
					}
					if(fallOff)
					{
						if(rMOB.riding()!=null)
							rMOB.tell(_("You fall off @x1!",rMOB.riding().name()));
						rMOB.setRiding(null);
					}
				}
				else
					rMOB.setRiding(null);
			}
			else
			if(rider instanceof Item)
			{
				final Item rItem=(Item)rider;
				if((rItem.owner()==sourceRoom)
				||(rItem.owner()==destRoom))
					destRoom.moveItemTo(rItem);
				else
					rItem.setRiding(null);
			}
		}
	}

	public static List<Rider> addRiders(Rider theRider, Rideable riding, List<Rider> riders)
	{

		if((riding!=null)&&(riding.mobileRideBasis()))
			for(int r=0;r<riding.numRiders();r++)
			{
				final Rider rider=riding.fetchRider(r);
				if((rider!=null)
				&&(rider!=theRider)
				&&(!riders.contains(rider)))
				{
					riders.add(rider);
					if(rider instanceof Rideable)
						addRiders(theRider,(Rideable)rider,riders);
				}
			}
		return riders;
	}

	public List<Rider> ridersAhead(Rider theRider, Room sourceRoom, Room destRoom, int directionCode, boolean flee, boolean running)
	{
		final LinkedList<Rider> riders=new LinkedList<Rider>();
		Rideable riding=theRider.riding();
		final LinkedList<Rideable> rideables=new LinkedList<Rideable>();
		while((riding!=null)&&(riding.mobileRideBasis()))
		{
			rideables.add(riding);
			addRiders(theRider,riding,riders);
			if((riding instanceof Rider)&&((Rider)riding).riding()!=theRider.riding())
				riding=((Rider)riding).riding();
			else
				riding=null;
		}
		if(theRider instanceof Rideable)
			addRiders(theRider,(Rideable)theRider,riders);
		for(final Iterator<Rider> r=riders.descendingIterator(); r.hasNext(); )
		{
			final Rider R=r.next();
			if((R instanceof Rideable)&&(((Rideable)R).numRiders()>0))
			{
				if(!rideables.contains(R))
					rideables.add((Rideable)R);
				r.remove();
			}
		}
		for(final ListIterator<Rideable> r=rideables.listIterator(); r.hasNext();)
		{
			riding=r.next();
			if((riding instanceof Item)
			&&((sourceRoom).isContent((Item)riding)))
				destRoom.moveItemTo((Item)riding);
			else
			if((riding instanceof MOB)
			&&((sourceRoom).isInhabitant((MOB)riding)))
			{
				final String inDir=((sourceRoom instanceof SpaceShip)||(sourceRoom.getArea() instanceof SpaceShip))?
						Directions.getShipDirectionName(directionCode):Directions.getDirectionName(directionCode);
				((MOB)riding).tell(_("You are ridden @x1.",inDir));
				if(!move(((MOB)riding),directionCode,false,false,true,false,running))
				{
					if(theRider instanceof MOB)
						((MOB)theRider).tell(_("@x1 won't seem to let you go that way.",((MOB)riding).name()));
					for(;r.hasPrevious();)
					{
						riding=r.previous();
						if((riding instanceof Item)
						&&((destRoom).isContent((Item)riding)))
							sourceRoom.moveItemTo((Item)riding);
						else
						if((riding instanceof MOB)
						&&(((MOB)riding).isMonster())
						&&((destRoom).isInhabitant((MOB)riding)))
							sourceRoom.bringMobHere((MOB)riding,false);
					}
					return null;
				}
			}
		}
		return riders;
	}

	@Override
	public boolean walk(MOB mob, int directionCode, boolean flee, boolean nolook, boolean noriders)
	{
		return walk(mob,directionCode,flee,nolook,noriders,false);
	}

	@Override
	public boolean run(MOB mob, int directionCode, boolean flee, boolean nolook, boolean noriders)
	{
		return run(mob,directionCode,flee,nolook,noriders,false);
	}

	@Override
	public boolean walk(MOB mob, int directionCode, boolean flee, boolean nolook, boolean noriders, boolean always)
	{
		return move(mob,directionCode,flee,nolook,noriders,always,false);
	}

	@Override
	public boolean run(MOB mob, int directionCode, boolean flee, boolean nolook, boolean noriders, boolean always)
	{
		return move(mob,directionCode,flee,nolook,noriders,always,true);
	}

	public boolean move(final MOB mob, final int directionCode, final boolean flee, final boolean nolook, final boolean noriders, final boolean always, final boolean running)
	{
		if(directionCode<0) return false;
		if(mob==null) return false;
		final Room thisRoom=mob.location();
		if(thisRoom==null) return false;
		final Room destRoom=thisRoom.getRoomInDir(directionCode);
		final Exit exit=thisRoom.getExitInDir(directionCode);
		if(destRoom==null)
		{
			mob.tell(_("You can't go that way."));
			return false;
		}

		final Exit opExit=thisRoom.getReverseExit(directionCode);
		final boolean useShipDirs=((thisRoom instanceof SpaceShip)||(thisRoom.getArea() instanceof SpaceShip));
		final int opDir=Directions.getOpDirectionCode(directionCode);
		final String dirName=useShipDirs?Directions.getShipDirectionName(directionCode):Directions.getDirectionName(directionCode);
		final String fromDir=useShipDirs?Directions.getShipFromDirectionName(opDir):Directions.getFromDirectionName(opDir);
		final String directionName=(directionCode==Directions.GATE)&&(exit!=null)?"through "+exit.name():dirName;
		final String otherDirectionName=(Directions.getOpDirectionCode(directionCode)==Directions.GATE)&&(exit!=null)?exit.name():fromDir;

		final int generalMask=always?CMMsg.MASK_ALWAYS:0;
		final int leaveCode;
		if(flee)
			leaveCode=generalMask|CMMsg.MSG_FLEE;
		else
			leaveCode=generalMask|CMMsg.MSG_LEAVE;

		final CMMsg enterMsg;
		final CMMsg leaveMsg;
		if((mob.riding()!=null)&&(mob.riding().mobileRideBasis()))
		{
			enterMsg=CMClass.getMsg(mob,destRoom,exit,generalMask|CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,_("<S-NAME> ride(s) @x1 in from @x2.",mob.riding().name(),otherDirectionName));
			leaveMsg=CMClass.getMsg(mob,thisRoom,opExit,leaveCode,((flee)?"You flee "+directionName+".":null),leaveCode,null,leaveCode,((flee)?"<S-NAME> flee(s) with "+mob.riding().name()+" "+directionName+".":"<S-NAME> ride(s) "+mob.riding().name()+" "+directionName+"."));
		}
		else
		{
			enterMsg=CMClass.getMsg(mob,destRoom,exit,generalMask|CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,null,CMMsg.MSG_ENTER,_("<S-NAME> @x1 from @x2.",CMLib.flags().dispositionString(mob,CMFlagLibrary.flag_arrives),otherDirectionName));
			leaveMsg=CMClass.getMsg(mob,thisRoom,opExit,leaveCode,((flee)?"You flee "+directionName+".":null),leaveCode,null,leaveCode,((flee)?"<S-NAME> flee(s) "+directionName+".":"<S-NAME> "+CMLib.flags().dispositionString(mob,CMFlagLibrary.flag_leaves)+" "+directionName+"."));
		}
		final boolean gotoAllowed=(!mob.isMonster()) && CMSecurity.isAllowed(mob,destRoom,CMSecurity.SecFlag.GOTO);
		if((exit==null)&&(!gotoAllowed))
		{
			mob.tell(_("You can't go that way."));
			return false;
		}
		else
		if(exit==null)
			thisRoom.showHappens(CMMsg.MSG_OK_VISUAL,_("The area to the @x1 shimmers and becomes transparent.",directionName));
		else
		if((!exit.okMessage(mob,enterMsg))&&(!gotoAllowed))
			return false;
		else
		if(!leaveMsg.target().okMessage(mob,leaveMsg)&&(!gotoAllowed))
			return false;
		else
		if((opExit!=null)&&(!opExit.okMessage(mob,leaveMsg))&&(!gotoAllowed))
			return false;
		else
		if(!enterMsg.target().okMessage(mob,enterMsg)&&(!gotoAllowed))
			return false;
		else
		if(!mob.okMessage(mob,enterMsg)&&(!gotoAllowed))
			return false;

		if(mob.riding()!=null)
		{
			if((!mob.riding().okMessage(mob,enterMsg))&&(!gotoAllowed))
				return false;
		}
		else
		{
			if(!mob.isMonster())
			{
				final int expense = running
										? CMProps.getIntVar(CMProps.Int.RUNCOST)
										: CMProps.getIntVar(CMProps.Int.WALKCOST);
				for(int i=0;i<expense;i++)
					CMLib.combat().expendEnergy(mob,true);
			}
			if((!flee)&&(mob.curState().getMovement()<=0)&&(!gotoAllowed))
			{
				mob.tell(_("You are too tired."));
				return false;
			}
			if((mob.soulMate()==null)&&(mob.playerStats()!=null)&&(mob.riding()==null)&&(mob.location()!=null))
				mob.playerStats().adjHygiene(mob.location().pointsPerMove(mob));
		}

		List<Rider> riders=null;
		if(!noriders)
		{
			riders=ridersAhead(mob,(Room)leaveMsg.target(),(Room)enterMsg.target(),directionCode,flee, running);
			if(riders==null) return false;
		}
		List<CMMsg> enterTrailersSoFar=null;
		List<CMMsg> leaveTrailersSoFar=null;
		if((leaveMsg.trailerMsgs()!=null)&&(leaveMsg.trailerMsgs().size()>0))
		{
			leaveTrailersSoFar=new LinkedList<CMMsg>();
			leaveTrailersSoFar.addAll(leaveMsg.trailerMsgs());
			leaveMsg.trailerMsgs().clear();
		}
		if((enterMsg.trailerMsgs()!=null)&&(enterMsg.trailerMsgs().size()>0))
		{
			enterTrailersSoFar=new LinkedList<CMMsg>();
			enterTrailersSoFar.addAll(enterMsg.trailerMsgs());
			enterMsg.trailerMsgs().clear();
		}
		if(exit!=null) exit.executeMsg(mob,enterMsg);
		if(mob.location()!=null)  mob.location().delInhabitant(mob);
		((Room)leaveMsg.target()).send(mob,leaveMsg);

		if(enterMsg.target()==null)
		{
			((Room)leaveMsg.target()).bringMobHere(mob,false);
			mob.tell(_("You can't go that way."));
			return false;
		}
		mob.setLocation((Room)enterMsg.target());
		((Room)enterMsg.target()).addInhabitant(mob);
		((Room)enterMsg.target()).send(mob,enterMsg);

		if(opExit!=null) opExit.executeMsg(mob,leaveMsg);

		if(!nolook)
		{
			CMLib.commands().postLook(mob,true);
			if((!mob.isMonster())
			&&(CMath.bset(mob.getBitmap(),MOB.ATT_AUTOWEATHER))
			&&(((Room)enterMsg.target())!=null)
			&&((thisRoom.domainType()&Room.INDOORS)>0)
			&&((((Room)enterMsg.target()).domainType()&Room.INDOORS)==0)
			&&(((Room)enterMsg.target()).getArea().getClimateObj().weatherType(((Room)enterMsg.target()))!=Climate.WEATHER_CLEAR)
			&&(((Room)enterMsg.target()).isInhabitant(mob)))
				mob.tell("\n\r"+((Room)enterMsg.target()).getArea().getClimateObj().weatherDescription(((Room)enterMsg.target())));
		}

		if(!noriders)
			ridersBehind(riders,(Room)leaveMsg.target(),(Room)enterMsg.target(),directionCode,flee, running);

		if(!flee)
		for(int f=0;f<mob.numFollowers();f++)
		{
			final MOB follower=mob.fetchFollower(f);
			if(follower!=null)
			{
				if((follower.amFollowing()==mob)
				&&((follower.location()==thisRoom)||(follower.location()==destRoom)))
				{
					if((follower.location()==thisRoom)&&(CMLib.flags().aliveAwakeMobile(follower,true)))
					{
						if(CMath.bset(follower.getBitmap(),MOB.ATT_AUTOGUARD))
							thisRoom.show(follower,null,null,CMMsg.MSG_OK_ACTION,_("<S-NAME> remain(s) on guard here."));
						else
						{
							final String inDir=((thisRoom instanceof SpaceShip)||(thisRoom.getArea() instanceof SpaceShip))?
									Directions.getShipDirectionName(directionCode):Directions.getDirectionName(directionCode);
							follower.tell(_("You follow @x1 @x2.",mob.name(follower),inDir));
							if(!move(follower,directionCode,false,false,false,false, running))
							{
								//follower.setFollowing(null);
							}
						}
					}
				}
				//else
				//	follower.setFollowing(null);
			}
		}
		if((leaveTrailersSoFar!=null)&&(leaveMsg.target() instanceof Room))
			for(final CMMsg msg : leaveTrailersSoFar)
				((Room)leaveMsg.target()).send(mob,msg);
		if((enterTrailersSoFar!=null)&&(enterMsg.target() instanceof Room))
			for(final CMMsg msg : enterTrailersSoFar)
				((Room)enterMsg.target()).send(mob,msg);
		return true;
	}

	@Override
	public boolean walk(MOB mob, int directionCode, boolean flee, boolean nolook)
	{
		return walk(mob,directionCode,flee,nolook,false);
	}

	@Override
	public int findExitDir(MOB mob, Room R, String desc)
	{
		int dir=Directions.getGoodDirectionCode(desc);
		if(dir<0)
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
		{
			final Exit e=R.getExitInDir(d);
			final Room r=R.getRoomInDir(d);
			if((e!=null)&&(r!=null))
			{
				if((CMLib.flags().canBeSeenBy(e,mob))
				&&((e.name().equalsIgnoreCase(desc))
				||(e.displayText().equalsIgnoreCase(desc))
				||(r.displayText(mob).equalsIgnoreCase(desc))
				||(e.description().equalsIgnoreCase(desc))))
				{
					dir=d; break;
				}
			}
		}
		if(dir<0)
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
		{
			final Exit e=R.getExitInDir(d);
			final Room r=R.getRoomInDir(d);
			if((e!=null)&&(r!=null))
			{
				if((CMLib.flags().canBeSeenBy(e,mob))
				&&(((CMLib.english().containsString(e.name(),desc))
				||(CMLib.english().containsString(e.displayText(),desc))
				||(CMLib.english().containsString(r.displayText(),desc))
				||(CMLib.english().containsString(e.description(),desc)))))
				{
					dir=d; break;
				}
			}
		}
		return dir;
	}
	@Override
	public int findRoomDir(MOB mob, Room R)
	{
		if((mob==null)||(R==null))
			return -1;
		final Room R2=mob.location();
		if(R2==null)
			return -1;
		final int dir=-1;
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
			if(R2.getRoomInDir(d)==R)
				return d;
		return dir;
	}

	@Override
	public List<Integer> getShortestTrail(final List<List<Integer>> finalSets)
	{
		if((finalSets==null)||(finalSets.size()==0)) return null;
		List<Integer> shortest=finalSets.get(0);
		for(int i=1;i<finalSets.size();i++)
			if(finalSets.get(i).size()<shortest.size())
				shortest=finalSets.get(i);
		return shortest;
	}

	@Override
	public List<List<Integer>> findAllTrails(final Room from, final Room to, final List<Room> radiantTrail)
	{
		final List<List<Integer>> finalSets=new Vector<List<Integer>>();
		if((from==null)||(to==null)||(from==to)) return finalSets;
		final int index=radiantTrail.indexOf(to);
		if(index<0) return finalSets;
		Room R=null;
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
		{
			R=to.getRoomInDir(d);
			if(R!=null)
			{
				if((R==from)&&(from.getRoomInDir(Directions.getOpDirectionCode(d))==to))
				{
					finalSets.add(new XVector<Integer>(Integer.valueOf(Directions.getOpDirectionCode(d))));
					return finalSets;
				}
				final int dex=radiantTrail.indexOf(R);
				if((dex>=0)&&(dex<index)&&(R.getRoomInDir(Directions.getOpDirectionCode(d))==to))
				{
					final List<List<Integer>> allTrailsBack=findAllTrails(from,R,radiantTrail);
					for(int a=0;a<allTrailsBack.size();a++)
					{
						final List<Integer> thisTrail=allTrailsBack.get(a);
						thisTrail.add(Integer.valueOf(Directions.getOpDirectionCode(d)));
						finalSets.add(thisTrail);
					}
				}
			}
		}
		return finalSets;
	}

	@Override
	public List<List<Integer>> findAllTrails(final Room from, final List<Room> tos, final List<Room> radiantTrail)
	{
		final List<List<Integer>> finalSets=new Vector<List<Integer>>();
		if(from==null) return finalSets;
		Room to=null;
		for(int t=0;t<tos.size();t++)
		{
			to=tos.get(t);
			finalSets.addAll(findAllTrails(from,to,radiantTrail));
		}
		return finalSets;
	}

	protected int getRoomDirection(Room R, Room toRoom, List<Room> ignore)
	{
		for(int d=Directions.NUM_DIRECTIONS()-1;d>=0;d--)
			if((R.getRoomInDir(d)==toRoom)
			&&(R!=toRoom)
			&&(!ignore.contains(R)))
				return d;
		return -1;
	}

	@Override
	public String getTrailToDescription(Room R1, List<Room> set, String where, boolean areaNames, boolean confirm, int radius, Set<Room> ignoreRooms, int maxMins)
	{
		Room R2=CMLib.map().getRoom(where);
		if(R2==null)
			for(final Enumeration<Area> a=CMLib.map().areas();a.hasMoreElements();)
			{
				final Area A=a.nextElement();
				if(A.name().equalsIgnoreCase(where))
				{
					if(set.size()==0)
					{
						int lowest=Integer.MAX_VALUE;
						for(final Enumeration<Room> r=A.getCompleteMap();r.hasMoreElements();)
						{
							final Room R=r.nextElement();
							final int x=R.roomID().indexOf('#');
							if((x>=0)&&(CMath.s_int(R.roomID().substring(x+1))<lowest))
								lowest=CMath.s_int(R.roomID().substring(x+1));
						}
						if(lowest<Integer.MAX_VALUE)
							R2=CMLib.map().getRoom(A.name()+"#"+lowest);
					}
					else
					{
						for(int i=0;i<set.size();i++)
						{
							final Room R=set.get(i);
							if(R.getArea()==A)
							{
								R2=R;
								break;
							}
						}
					}
					break;
				}
			}
		if(R2==null) return "Unable to determine '"+where+"'.";
		final TrackingLibrary.TrackingFlags flags = new TrackingLibrary.TrackingFlags()
											.plus(TrackingLibrary.TrackingFlag.NOEMPTYGRIDS);
		if(set.size()==0)
			getRadiantRooms(R1,set,flags,R2,radius,ignoreRooms);
		int foundAt=-1;
		for(int i=0;i<set.size();i++)
		{
			final Room R=set.get(i);
			if(R==R2){ foundAt=i; break;}
		}
		if(foundAt<0) return "You can't get to '"+R2.roomID()+"' from here.";
		Room checkR=R2;
		final List<Room> trailV=new Vector<Room>();
		trailV.add(R2);
		final HashSet<Area> areasDone=new HashSet<Area>();
		boolean didSomething=false;
		final long startTime = System.currentTimeMillis();
		while(checkR!=R1)
		{
			final long waitTime = System.currentTimeMillis() - startTime;
			if(waitTime > (1000 * 60 * (maxMins)))
				return "You can't get there from here.";
			didSomething=false;
			for(int r=foundAt-1;r>=0;r--)
			{
				final Room R=set.get(r);
				if(getRoomDirection(R,checkR,trailV)>=0)
				{
					trailV.add(R);
					if(!areasDone.contains(R.getArea()))
						areasDone.add(R.getArea());
					foundAt=r;
					checkR=R;
					didSomething=true;
					break;
				}
			}
			if(!didSomething)
				return "You can't get there from here.";
		}
		final List<String> theDirTrail=new Vector<String>();
		final List<Room> empty=new ReadOnlyVector<Room>();
		for(int s=trailV.size()-1;s>=1;s--)
		{
			final Room R=trailV.get(s);
			final Room RA=trailV.get(s-1);
			theDirTrail.add(Directions.getDirectionChar(getRoomDirection(R,RA,empty))+" ");
		}
		final StringBuffer theTrail=new StringBuffer("");
		if(confirm)	theTrail.append("\n\r"+CMStrings.padRight(_("Trail"),30)+": ");
		String lastDir="";
		int lastNum=0;
		while(theDirTrail.size()>0)
		{
			final String s=theDirTrail.get(0);
			if(lastNum==0)
			{
				lastDir=s;
				lastNum=1;
			}
			else
			if(s.equalsIgnoreCase(lastDir))
				lastNum++;
			else
			{
				if(lastNum==1)
					theTrail.append(lastDir+" ");
				else
					theTrail.append(Integer.toString(lastNum)+lastDir+" ");
				lastDir=s;
				lastNum=1;
			}
			theDirTrail.remove(0);
		}
		if(lastNum==1)
			theTrail.append(lastDir);
		else
		if(lastNum>0)
			theTrail.append(Integer.toString(lastNum)+lastDir);

		if((confirm)&&(trailV.size()>1))
		{
			for(int i=0;i<trailV.size();i++)
			{
				final Room R=trailV.get(i);
				if(R.roomID().length()==0)
				{
					theTrail.append("*");
					break;
				}
			}
			final Room R=trailV.get(1);
			theTrail.append("\n\r"+CMStrings.padRight(_("From"),30)+": "+Directions.getDirectionName(getRoomDirection(R,R2,empty))+" <- "+R.roomID());
			theTrail.append("\n\r"+CMStrings.padRight(_("Room"),30)+": "+R.displayText()+"/"+R.description());
			theTrail.append("\n\r\n\r");
		}
		if((areaNames)&&(areasDone.size()>0))
		{
			theTrail.append("\n\r"+CMStrings.padRight(_("Areas"),30)+":");
			for (final Area A : areasDone)
			{
				theTrail.append(" \""+A.name()+"\",");
			}
		}
		if(theTrail.toString().trim().length()==0)
			return "You can't get there from here.";
		return theTrail.toString();
	}


}
