package com.planet_ink.coffee_mud.Common;
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
import com.planet_ink.coffee_mud.Libraries.interfaces.DatabaseEngine;
import com.planet_ink.coffee_mud.Libraries.interfaces.XMLLibrary;
import com.planet_ink.coffee_mud.Libraries.interfaces.DatabaseEngine.PlayerData;
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
public class DefaultTimeClock implements TimeClock
{
	public String ID(){return "DefaultTimeClock";}
	public String name(){return "Time Object";}
	public CMObject newInstance(){try{return getClass().newInstance();}catch(Exception e){return new DefaultTimeClock();}}
	public void initializeClass(){}
	
	protected int tickStatus=Tickable.STATUS_NOT;
	public int getTickStatus(){return tickStatus;}
	protected boolean loaded=false;
	protected String loadName=null;
	public void setLoadName(String name){loadName=name;}
	protected int year=1;
	protected int month=1;
	protected int day=1;
	protected int time=0;
	protected int hoursInDay=6;
	protected String[] monthsInYear={
			 "the 1st month","the 2nd month","the 3rd month","the 4th month",
			 "the 5th month","the 6th month","the 7th month","the 8th month"
	};
	protected int daysInMonth=20;
	protected int[] dawnToDusk={0,1,4,6};
	protected String[] weekNames={};
	protected String[] yearNames={"year #"};
	
	public int getHoursInDay(){return hoursInDay;}
	public void setHoursInDay(int h){hoursInDay=h;}
	public int getDaysInMonth(){return daysInMonth;}
	public void setDaysInMonth(int d){daysInMonth=d;}
	public int getMonthsInYear(){return monthsInYear.length;}
	public String[] getMonthNames(){return monthsInYear;}
	public void setMonthsInYear(String[] months){monthsInYear=months;}
	public int[] getDawnToDusk(){return dawnToDusk;}
	public String[] getYearNames(){return yearNames;}
	public void setYearNames(String[] years){yearNames=years;}
	public void setDawnToDusk(int dawn, int day, int dusk, int night)
	{ 
		dawnToDusk[TimeOfDay.DAWN.ordinal()]=dawn;
		dawnToDusk[TimeOfDay.DAY.ordinal()]=day;
		dawnToDusk[TimeOfDay.DUSK.ordinal()]=dusk;
		dawnToDusk[TimeOfDay.NIGHT.ordinal()]=night;
	}
	public String[] getWeekNames(){return weekNames;}
	public int getDaysInWeek(){return weekNames.length;}
	public void setDaysInWeek(String[] days){weekNames=days;}
	
	public String getShortestTimeDescription()
	{
		StringBuffer timeDesc=new StringBuffer("");
		timeDesc.append(getYear());
		timeDesc.append("/"+getMonth());
		timeDesc.append("/"+getDayOfMonth());
		timeDesc.append(" HR:"+getHourOfDay());
		return timeDesc.toString();
	}
	public String getShortTimeDescription()
	{
		StringBuffer timeDesc=new StringBuffer("");
		timeDesc.append("hour "+getHourOfDay()+" on ");
		if(getDaysInWeek()>0)
		{
			long x=((long)getYear())*((long)getMonthsInYear())*getDaysInMonth();
			x=x+((long)(getMonth()-1))*((long)getDaysInMonth());
			x=x+getDayOfMonth();
			timeDesc.append(getWeekNames()[(int)(x%getDaysInWeek())]+", ");
		}
		timeDesc.append("the "+getDayOfMonth()+CMath.numAppendage(getDayOfMonth()));
		timeDesc.append(" day of "+getMonthNames()[getMonth()-1]);
		if(getYearNames().length>0)
			timeDesc.append(", "+CMStrings.replaceAll(getYearNames()[getYear()%getYearNames().length],"#",""+getYear()));
		return timeDesc.toString();
	}
	
	public void initializeINIClock(CMProps page)
	{
		if(CMath.s_int(page.getStr("HOURSINDAY"))>0)
			setHoursInDay(CMath.s_int(page.getStr("HOURSINDAY")));

		if(CMath.s_int(page.getStr("DAYSINMONTH"))>0)
			setDaysInMonth(CMath.s_int(page.getStr("DAYSINMONTH")));

		String monthsInYear=page.getStr("MONTHSINYEAR");
		if(monthsInYear.trim().length()>0)
			setMonthsInYear(CMParms.toStringArray(CMParms.parseCommas(monthsInYear,true)));

		setDaysInWeek(CMParms.toStringArray(CMParms.parseCommas(page.getStr("DAYSINWEEK"),true)));

		if(page.containsKey("YEARDESC"))
			setYearNames(CMParms.toStringArray(CMParms.parseCommas(page.getStr("YEARDESC"),true)));

		if(page.containsKey("DAWNHR")&&page.containsKey("DAYHR")
				&&page.containsKey("DUSKHR")&&page.containsKey("NIGHTHR"))
		setDawnToDusk(
						CMath.s_int(page.getStr("DAWNHR")),
						CMath.s_int(page.getStr("DAYHR")),
						CMath.s_int(page.getStr("DUSKHR")),
						CMath.s_int(page.getStr("NIGHTHR")));

		CMProps.setIntVar(CMProps.Int.TICKSPERMUDDAY,""+((CMProps.getMillisPerMudHour()*CMLib.time().globalClock().getHoursInDay()/CMProps.getTickMillis())));
		CMProps.setIntVar(CMProps.Int.TICKSPERMUDMONTH,""+((CMProps.getMillisPerMudHour()*CMLib.time().globalClock().getHoursInDay()*CMLib.time().globalClock().getDaysInMonth()/CMProps.getTickMillis())));
	}
	
	public String timeDescription(MOB mob, Room room)
	{
		StringBuffer timeDesc=new StringBuffer("");

		if(CMLib.flags().canSee(mob))
			timeDesc.append(getTODCode().getDesc());
		timeDesc.append("(Hour: "+getHourOfDay()+"/"+(getHoursInDay()-1)+")");
		timeDesc.append("\n\rIt is ");
		if(getDaysInWeek()>0)
		{
			long x=((long)getYear())*((long)getMonthsInYear())*getDaysInMonth();
			x=x+((long)(getMonth()-1))*((long)getDaysInMonth());
			x=x+getDayOfMonth();
			timeDesc.append(getWeekNames()[(int)(x%getDaysInWeek())]+", ");
		}
		timeDesc.append("the "+getDayOfMonth()+CMath.numAppendage(getDayOfMonth()));
		timeDesc.append(" day of "+getMonthNames()[getMonth()-1]);
		if(getYearNames().length>0)
			timeDesc.append(", "+CMStrings.replaceAll(getYearNames()[getYear()%getYearNames().length],"#",""+getYear()));
		timeDesc.append(".\n\rIt is "+getSeasonCode().toString().toLowerCase()+".");
		if((CMLib.flags().canSee(mob))
		&&(getTODCode()==TimeClock.TimeOfDay.NIGHT)
		&&(CMLib.map().hasASky(room)))
		{
			switch(room.getArea().getClimateObj().weatherType(room))
			{
			case Climate.WEATHER_BLIZZARD:
			case Climate.WEATHER_HAIL:
			case Climate.WEATHER_SLEET:
			case Climate.WEATHER_SNOW:
			case Climate.WEATHER_RAIN:
			case Climate.WEATHER_THUNDERSTORM:
				timeDesc.append("\n\r"+room.getArea().getClimateObj().weatherDescription(room)+" You can't see the moon."); break;
			case Climate.WEATHER_CLOUDY:
				timeDesc.append("\n\rThe clouds obscure the moon."); break;
			case Climate.WEATHER_DUSTSTORM:
				timeDesc.append("\n\rThe dust obscures the moon."); break;
			default:
				timeDesc.append("\n\r"+getMoonPhase().getDesc());
				break;
			}
		}
		return timeDesc.toString();
	}

	public int getYear()
	{
		return year;
	}
	
	public void setYear(int y)
	{
		year=y;
	}

	public Season getSeasonCode()
	{
		int div=(int)Math.round(Math.floor(CMath.div(getMonthsInYear(),4.0)));
		if(month<div) return TimeClock.Season.WINTER;
		if(month<(div*2)) return TimeClock.Season.SPRING;
		if(month<(div*3)) return TimeClock.Season.SUMMER;
		return TimeClock.Season.FALL;
	}
	
	public int getMonth()
	{
		return month;
	}
	
	public void setMonth(int m)
	{
		month=m;
	}
	
	public MoonPhase getMoonPhase()
	{
		return TimeClock.MoonPhase.values()[(int)Math.round(Math.floor(CMath.mul(CMath.div(getDayOfMonth(),getDaysInMonth()+1),8.0)))];
	}

	public int getDayOfMonth(){return day;}
	public void setDayOfMonth(int d){day=d;}
	public int getHourOfDay(){return time;}
	public TimeOfDay getTODCode()
	{
		if((time>=getDawnToDusk()[TimeClock.TimeOfDay.NIGHT.ordinal()])&&(getDawnToDusk()[TimeClock.TimeOfDay.NIGHT.ordinal()]>=0))
			return TimeClock.TimeOfDay.NIGHT;
		if((time>=getDawnToDusk()[TimeClock.TimeOfDay.DUSK.ordinal()])&&(getDawnToDusk()[TimeClock.TimeOfDay.DUSK.ordinal()]>=0))
			return TimeClock.TimeOfDay.DUSK;
		if((time>=getDawnToDusk()[TimeClock.TimeOfDay.DAY.ordinal()])&&(getDawnToDusk()[TimeClock.TimeOfDay.DAY.ordinal()]>=0))
			return TimeClock.TimeOfDay.DAY;
		if((time>=getDawnToDusk()[TimeClock.TimeOfDay.DAWN.ordinal()])&&(getDawnToDusk()[TimeClock.TimeOfDay.DAWN.ordinal()]>=0))
			return TimeClock.TimeOfDay.DAWN;
		// it's before night, dusk, day, and dawn... before dawn is still night.
		if(getDawnToDusk()[TimeClock.TimeOfDay.NIGHT.ordinal()]>=0)
			return TimeClock.TimeOfDay.NIGHT;
		return TimeClock.TimeOfDay.DAY;
	}
	public boolean setHourOfDay(int t)
	{
		TimeOfDay oldCode=getTODCode();
		time=t;
		return getTODCode()!=oldCode;
	}
	
	public CMObject copyOf()
	{
		try
		{
			TimeClock C=(TimeClock)this.clone();
			return C;
		}
		catch(CloneNotSupportedException e)
		{
			return new DefaultTimeClock();
		}
	}
	public TimeClock deriveClock(long millis)
	{
		try
		{
			TimeClock C=(TimeClock)this.clone();
			long diff=(System.currentTimeMillis()-millis)/CMProps.getMillisPerMudHour();
			C.tickTock((int)diff);
			return C;
		}
		catch(CloneNotSupportedException e)
		{
			
		}
		return CMLib.time().globalClock();
	}

	public String deriveEllapsedTimeString(long millis)
	{
		int hours=(int)(millis/CMProps.getMillisPerMudHour());
		int days=0;
		int months=0;
		int years=0;
		if(hours>getHoursInDay())
		{
			days=(int)Math.round(Math.floor(CMath.div(hours,getHoursInDay())));
			hours=hours-(days*getHoursInDay());
		}
		if(days>getDaysInMonth())
		{
			months=(int)Math.round(Math.floor(CMath.div(days,getDaysInMonth())));
			days=days-(months*getDaysInMonth());
		}
		if(months>getMonthsInYear())
		{
			years=(int)Math.round(Math.floor(CMath.div(months,getMonthsInYear())));
			months=months-(years*getMonthsInYear());
		}
		StringBuffer buf=new StringBuffer("");
		if(years>0) buf.append(years+" years");
		if(months>0)
		{
			if(buf.length()>0) buf.append(", ");
			buf.append(months+" months");
		}
		if(days>0)
		{
			if(buf.length()>0) buf.append(", ");
			buf.append(days+" days");
		}
		if(hours>0)
		{
			if(buf.length()>0) buf.append(", ");
			buf.append(hours+" hours");
		}
		if(buf.length()==0) return "any second now";
		return buf.toString();
	}
	
	public long deriveMillisAfter(TimeClock C)
	{
		long numMudHours=0;
		if(C.getYear()>getYear()) return -1;
		else 
		if(C.getYear()==getYear())
			if(C.getMonth()>getMonth()) return -1;
			else 
			if(C.getMonth()==getMonth())
				if(C.getDayOfMonth()>getDayOfMonth()) return -1;
				else 
				if(C.getDayOfMonth()==getDayOfMonth())
					if(C.getHourOfDay()>getHourOfDay()) return -1;
		numMudHours+=(getYear()-C.getYear())*(getHoursInDay()*getDaysInMonth()*getMonthsInYear());
		numMudHours+=(getMonth()-C.getMonth())*(getHoursInDay()*getDaysInMonth());
		numMudHours+=(getDayOfMonth()-C.getDayOfMonth())*getHoursInDay();
		numMudHours+=(getHourOfDay()-C.getHourOfDay());
		return numMudHours*CMProps.getMillisPerMudHour();
	}
	
	public void handleTimeChange()
	{
		try
		{
			for(Enumeration<Area> a=CMLib.map().areas();a.hasMoreElements();)
			{
				Area A=a.nextElement();
				if(A.getTimeObj()==this)
				for(Enumeration<Room> r=A.getProperMap();r.hasMoreElements();)
				{
					Room R=r.nextElement();
					if((R!=null)&&((R.numInhabitants()>0)||(R.numItems()>0)))
					{
						R.recoverPhyStats();
						for(int m=0;m<R.numInhabitants();m++)
						{
							MOB mob=R.fetchInhabitant(m);
							if((mob!=null)
							&&(!mob.isMonster()))
							{
								if(CMLib.map().hasASky(R)
								&&(!CMLib.flags().isSleeping(mob))
								&&(CMLib.flags().canSee(mob)))
								{
									final String message = CMProps.getListFileValue(CMProps.ListFile.TOD_CHANGE_OUTSIDE, getTODCode().ordinal());
									if(message.trim().length()>0)
										mob.tell(message);
								}
								else
								{
									final String message = CMProps.getListFileValue(CMProps.ListFile.TOD_CHANGE_INSIDE, getTODCode().ordinal());
									if(message.trim().length()>0)
										mob.tell(message);
								}
							}
						}
					}
					if(R!=null)
						R.recoverRoomStats();
				}
			}
		}catch(java.util.NoSuchElementException x){}
	}

	public void tickTock(int howManyHours)
	{
		TimeOfDay todCode=getTODCode();
		if(howManyHours!=0)
		{
			setHourOfDay(getHourOfDay()+howManyHours);
			lastTicked=System.currentTimeMillis();
			while(getHourOfDay()>=getHoursInDay())
			{
				setHourOfDay(getHourOfDay()-getHoursInDay());
				setDayOfMonth(getDayOfMonth()+1);
				if(getDayOfMonth()>getDaysInMonth())
				{
					setDayOfMonth(1);
					setMonth(getMonth()+1);
					if(getMonth()>getMonthsInYear())
					{
						setMonth(1);
						setYear(getYear()+1);
					}
				}
			}
			while(getHourOfDay()<0)
			{
				setHourOfDay(getHoursInDay()+getHourOfDay());
				setDayOfMonth(getDayOfMonth()-1);
				if(getDayOfMonth()<1)
				{
					setDayOfMonth(getDaysInMonth());
					setMonth(getMonth()-1);
					if(getMonth()<1)
					{
						setMonth(getMonthsInYear());
						setYear(getYear()-1);
					}
				}
			}
		}
		if(getTODCode()!=todCode) handleTimeChange();
	}
	public void save()
	{
		if((loaded)&&(loadName!=null))
		{
			CMLib.database().DBReCreateData(loadName,"TIMECLOCK","TIMECLOCK/"+loadName,
			"<DAY>"+getDayOfMonth()+"</DAY><MONTH>"+getMonth()+"</MONTH><YEAR>"+getYear()+"</YEAR>"
			+"<HOURS>"+getHoursInDay()+"</HOURS><DAYS>"+getDaysInMonth()+"</DAYS>"
			+"<MONTHS>"+CMParms.toStringList(getMonthNames())+"</MONTHS>"
			+"<DAWNHR>"+getDawnToDusk()[TimeOfDay.DAWN.ordinal()]+"</DAWNHR>"
			+"<DAYHR>"+getDawnToDusk()[TimeOfDay.DAY.ordinal()]+"</DAYHR>"
			+"<DUSKHR>"+getDawnToDusk()[TimeOfDay.DUSK.ordinal()]+"</DUSKHR>"
			+"<NIGHTHR>"+getDawnToDusk()[TimeOfDay.NIGHT.ordinal()]+"</NIGHTHR>"
			+"<WEEK>"+CMParms.toStringList(getWeekNames())+"</WEEK>"
			+"<YEARS>"+CMParms.toStringList(getYearNames())+"</YEARS>"
			);
		}
	}

	public long lastTicked=0;
	public boolean tick(Tickable ticking, int tickID)
	{
		tickStatus=Tickable.STATUS_NOT;
		if(((loadName==null)||(loaded))
		&&(((System.currentTimeMillis()-lastTicked)<=CMProps.getMillisPerMudHour())))
			return true;
		synchronized(this)
		{
			boolean timeToTick = ((System.currentTimeMillis()-lastTicked)>CMProps.getMillisPerMudHour());
			lastTicked=System.currentTimeMillis();
			if((loadName!=null)&&(!loaded))
			{
				loaded=true;
				List<PlayerData> bitV=CMLib.database().DBReadData(loadName,"TIMECLOCK");
				String timeRsc=null;
				if((bitV==null)||(bitV.size()==0))
					timeRsc="<TIME>-1</TIME><DAY>1</DAY><MONTH>1</MONTH><YEAR>1</YEAR>";
				else
					timeRsc=bitV.get(0).xml;
				List<XMLLibrary.XMLpiece> V=CMLib.xml().parseAllXML(timeRsc);
				setHourOfDay(CMLib.xml().getIntFromPieces(V,"TIME"));
				setDayOfMonth(CMLib.xml().getIntFromPieces(V,"DAY"));
				setMonth(CMLib.xml().getIntFromPieces(V,"MONTH"));
				setYear(CMLib.xml().getIntFromPieces(V,"YEAR"));
				if(this!=CMLib.time().globalClock())
				{
					if((CMLib.xml().getValFromPieces(V,"HOURS").length()==0)
					||(CMLib.xml().getValFromPieces(V,"DAYS").length()==0)
					||(CMLib.xml().getValFromPieces(V,"MONTHS").length()==0))
					{
						setHoursInDay(CMLib.time().globalClock().getHoursInDay());
						setDaysInMonth(CMLib.time().globalClock().getDaysInMonth());
						setMonthsInYear(CMLib.time().globalClock().getMonthNames());
						setDawnToDusk(CMLib.time().globalClock().getDawnToDusk()[TimeOfDay.DAWN.ordinal()],
									  CMLib.time().globalClock().getDawnToDusk()[TimeOfDay.DAY.ordinal()],
									  CMLib.time().globalClock().getDawnToDusk()[TimeOfDay.DUSK.ordinal()],
									  CMLib.time().globalClock().getDawnToDusk()[TimeOfDay.NIGHT.ordinal()]);
						setDaysInWeek(CMLib.time().globalClock().getWeekNames());
						setYearNames(CMLib.time().globalClock().getYearNames());
					}
					else
					{
						setHoursInDay(CMLib.xml().getIntFromPieces(V,"HOURS"));
						setDaysInMonth(CMLib.xml().getIntFromPieces(V,"DAYS"));
						setMonthsInYear(CMParms.toStringArray(CMParms.parseCommas(CMLib.xml().getValFromPieces(V,"MONTHS"),true)));
						setDawnToDusk(CMLib.xml().getIntFromPieces(V,"DAWNHR"),
									  CMLib.xml().getIntFromPieces(V,"DAYHR"),
									  CMLib.xml().getIntFromPieces(V,"DUSKHR"),
									  CMLib.xml().getIntFromPieces(V,"NIGHTHR"));
						setDaysInWeek(CMParms.toStringArray(CMParms.parseCommas(CMLib.xml().getValFromPieces(V,"WEEK"),true)));
						setYearNames(CMParms.toStringArray(CMParms.parseCommas(CMLib.xml().getValFromPieces(V,"YEARS"),true)));
					}
				}
			}
			if(timeToTick)
				tickTock(1);
		}
		return true;
	}
	public int compareTo(CMObject o){ return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));}
}
