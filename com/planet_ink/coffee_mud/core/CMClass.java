package com.planet_ink.coffee_mud.core;
import com.planet_ink.coffee_mud.WebMacros.interfaces.*;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
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
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.net.URL;

import org.mozilla.javascript.*;
import org.mozilla.javascript.optimizer.*;


/*
   Copyright 2000-2011 Bo Zimmerman

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
public class CMClass extends ClassLoader
{
	protected static boolean debugging=false;
    protected static final Map<String,Class<?>> classes=new Hashtable<String,Class<?>>();
    
    private static CMClass[] clss=new CMClass[256];
    public CMClass()
    {
        super();
        char c=Thread.currentThread().getThreadGroup().getName().charAt(0);
        if(clss==null) clss=new CMClass[256];
        if(clss[c]==null) clss[c]=this;
    }
    private static CMClass c(){ return clss[Thread.currentThread().getThreadGroup().getName().charAt(0)];}
    public static CMClass c(char c){return clss[c];}
    public static CMClass instance(){return c();}
    public static boolean[] classLoaderSync={false};

    public static final int OBJECT_RACE=0;
    public static final int OBJECT_CHARCLASS=1;
    public static final int OBJECT_MOB=2;
    public static final int OBJECT_ABILITY=3;
    public static final int OBJECT_LOCALE=4;
    public static final int OBJECT_EXIT=5;
    public static final int OBJECT_ITEM=6;
    public static final int OBJECT_BEHAVIOR=7;
    public static final int OBJECT_CLAN=8;
    public static final int OBJECT_WEAPON=9;
    public static final int OBJECT_ARMOR=10;
    public static final int OBJECT_MISCMAGIC=11;
    public static final int OBJECT_AREA=12;
    public static final int OBJECT_COMMAND=13;
    public static final int OBJECT_CLANITEMS=14;
    public static final int OBJECT_MISCTECH=15;
    public static final int OBJECT_WEBMACROS=16;
    public static final int OBJECT_COMMON=17;
    public static final int OBJECT_LIBRARY=18;
    public static final int OBJECT_TOTAL=19;
    
    public static final int[] OBJECTS_ITEMTYPES = new int[]{
    	CMClass.OBJECT_MISCMAGIC,
    	CMClass.OBJECT_ITEM,
    	CMClass.OBJECT_ARMOR,
    	CMClass.OBJECT_CLANITEMS,
    	CMClass.OBJECT_MISCMAGIC,
    	CMClass.OBJECT_MISCTECH,
    	CMClass.OBJECT_WEAPON
    };
    
    public static int longestWebMacro=-1;

    
    protected Hashtable<String,CMCommon> common=new Hashtable<String,CMCommon>();
    protected XVector<Race> 			 races=new XVector<Race>();
    protected XVector<CharClass> 		 charClasses=new XVector<CharClass>();
    protected XVector<MOB> 				 MOBs=new XVector<MOB>();
    protected XVector<Ability> 			 abilities=new XVector<Ability>();
    protected XVector<Room> 			 locales=new XVector<Room>();
    protected XVector<Exit> 			 exits=new XVector<Exit>();
    protected XVector<Item> 			 items=new XVector<Item>();
    protected XVector<Behavior> 		 behaviors=new XVector<Behavior>();
    protected XVector<Weapon> 			 weapons=new XVector<Weapon>();
    protected XVector<Armor> 			 armor=new XVector<Armor>();
    protected XVector<MiscMagic> 		 miscMagic=new XVector<MiscMagic>();
    protected XVector<Electronics> 		 miscTech=new XVector<Electronics>();
    protected XVector<ClanItem> 		 clanItems=new XVector<ClanItem>();
    protected XVector<Area> 			 areaTypes=new XVector<Area>();
    protected XVector<Command> 			 commands=new XVector<Command>();
    protected XVector<CMLibrary> 		 libraries=new XVector<CMLibrary>();
    protected Hashtable<String,WebMacro> webMacros=new Hashtable<String,WebMacro>();
    protected Hashtable<String,Command>  commandWords=new Hashtable<String,Command>();
    
    protected static final long[] OBJECT_CREATIONS=new long[OBJECT_TOTAL];
    protected static final long[] OBJECT_DESTRUCTIONS=new long[OBJECT_TOTAL];
    
    protected static final Map<CMObject,Object>[] OBJECT_CACHE=new WeakHashMap[OBJECT_TOTAL];
    protected static final List<CMMsg> MSGS_CACHE=new Vector<CMMsg>();
    protected static final boolean KEEP_OBJECT_CACHE=false;
    
    static
    { 
    	if(KEEP_OBJECT_CACHE) 
    		for(int i=0;i<OBJECT_TOTAL;i++)
    			OBJECT_CACHE[i]=new WeakHashMap<CMObject,Object>();
    }
    
    public static final String[] OBJECT_DESCS=
    {
		"RACE","CHARCLASS","MOB","ABILITY","LOCALE","EXIT","ITEM","BEHAVIOR",
		"CLAN","WEAPON","ARMOR","MISCMAGIC","AREA","COMMAND","CLANITEMS",
		"MISCTECH","WEBMACROS","COMMON","LIBRARY"
	};
    
    protected static final String[] OBJECT_ANCESTORS=
    {
		"com.planet_ink.coffee_mud.Races.interfaces.Race",
		"com.planet_ink.coffee_mud.CharClasses.interfaces.CharClass",
		"com.planet_ink.coffee_mud.MOBS.interfaces.MOB",
		"com.planet_ink.coffee_mud.Abilities.interfaces.Ability",
		"com.planet_ink.coffee_mud.Locales.interfaces.Room",
		"com.planet_ink.coffee_mud.Exits.interfaces.Exit",
		"com.planet_ink.coffee_mud.Items.interfaces.Item",
		"com.planet_ink.coffee_mud.Behaviors.interfaces.Behavior",
		"com.planet_ink.coffee_mud.core.interfaces.Clan",
		"com.planet_ink.coffee_mud.Items.interfaces.Weapon",
		"com.planet_ink.coffee_mud.Items.interfaces.Armor",
		"com.planet_ink.coffee_mud.Items.interfaces.MiscMagic",
		"com.planet_ink.coffee_mud.Areas.interfaces.Area",
		"com.planet_ink.coffee_mud.Commands.interfaces.Command",
		"com.planet_ink.coffee_mud.Items.interfaces.ClanItem",
		"com.planet_ink.coffee_mud.Items.interfaces.Electronics",
		"com.planet_ink.coffee_mud.WebMacros.interfaces.WebMacro",
        "com.planet_ink.coffee_mud.Common.interfaces.CMCommon",
        "com.planet_ink.coffee_mud.Libraries.interfaces.CMLibrary",
	};
    
    public final static void bumpCounter(final CMObject O, final int which)
    {
        if(KEEP_OBJECT_CACHE)
        {
            if(OBJECT_CACHE[which].containsKey(O))
            {
                Log.errOut("Duplicate!",new Exception("Duplicate Found!"));
                return;
            }
            OBJECT_CACHE[which].put(O,OBJECT_CACHE);
        }
        OBJECT_CREATIONS[which]++;
    }

    public final static boolean isType(final Object O, final int type)
    {
        switch(type)
        {
            case OBJECT_RACE: return O instanceof Race;
            case OBJECT_CHARCLASS: return O instanceof CharClass;
            case OBJECT_MOB: return O instanceof MOB;
            case OBJECT_ABILITY: return O instanceof Ability;
            case OBJECT_LOCALE: return O instanceof Room;
            case OBJECT_EXIT: return O instanceof Exit;
            case OBJECT_ITEM: return O instanceof Item;
            case OBJECT_BEHAVIOR: return O instanceof Behavior;
            case OBJECT_CLAN: return O instanceof Clan;
            case OBJECT_WEAPON: return O instanceof Weapon;
            case OBJECT_ARMOR: return O instanceof Armor;
            case OBJECT_MISCMAGIC: return O instanceof MiscMagic;
            case OBJECT_AREA: return O instanceof Area;
            case OBJECT_COMMAND: return O instanceof Command;
            case OBJECT_CLANITEMS: return O instanceof ClanItem;
            case OBJECT_MISCTECH: return O instanceof Electronics;
            case OBJECT_WEBMACROS: return O instanceof WebMacro;
            case OBJECT_COMMON: return O instanceof CMCommon;
            case OBJECT_LIBRARY: return O instanceof CMLibrary;
        }
        return false;
    }

    public final static CMObject getByType(final String ID, final int type)
    {
        switch(type)
        {
            case OBJECT_RACE: return CMClass.getRace(ID);
            case OBJECT_CHARCLASS: return CMClass.getCharClass(ID);
            case OBJECT_MOB: return CMClass.getMOB(ID);
            case OBJECT_ABILITY: return CMClass.getAbility(ID);
            case OBJECT_LOCALE: return CMClass.getLocale(ID);
            case OBJECT_EXIT: return CMClass.getExit(ID);
            case OBJECT_ITEM: return CMClass.getBasicItem(ID);
            case OBJECT_BEHAVIOR: return CMClass.getBehavior(ID);
            case OBJECT_CLAN: return CMClass.getCommon(ID);
            case OBJECT_WEAPON: return CMClass.getWeapon(ID);
            case OBJECT_ARMOR: return CMClass.getAreaType(ID);
            case OBJECT_MISCMAGIC: return CMClass.getMiscMagic(ID);
            case OBJECT_AREA: return CMClass.getAreaType(ID);
            case OBJECT_COMMAND: return CMClass.getCommand(ID);
            case OBJECT_CLANITEMS: return CMClass.getClanItem(ID);
            case OBJECT_MISCTECH: return CMClass.getMiscMagic(ID);
            case OBJECT_WEBMACROS: return CMClass.getWebMacro(ID);
            case OBJECT_COMMON: return CMClass.getCommon(ID);
            case OBJECT_LIBRARY: return CMClass.getLibrary(ID);
        }
        return null;
    }

    public final static int getType(final Object O)
    {
    	if(O instanceof Race) return OBJECT_RACE;
    	if(O instanceof CharClass) return OBJECT_CHARCLASS;
    	if(O instanceof Ability) return OBJECT_ABILITY;
    	if(O instanceof Room) return OBJECT_LOCALE;
    	if(O instanceof MOB) return OBJECT_MOB;
    	if(O instanceof Exit) return OBJECT_EXIT;
    	if(O instanceof Behavior) return OBJECT_BEHAVIOR;
    	if(O instanceof WebMacro) return OBJECT_WEBMACROS;
    	if(O instanceof Area) return OBJECT_AREA;
    	if(O instanceof CMLibrary) return OBJECT_LIBRARY;
    	if(O instanceof CMCommon) return OBJECT_COMMON;
    	if(O instanceof Electronics) return OBJECT_MISCTECH;
    	if(O instanceof Command) return OBJECT_COMMAND;
    	if(O instanceof Clan) return OBJECT_CLAN;
    	if(O instanceof ClanItem) return OBJECT_CLANITEMS;
    	if(O instanceof MiscMagic) return OBJECT_MISCMAGIC;
    	if(O instanceof Armor) return OBJECT_ARMOR;
    	if(O instanceof Weapon) return OBJECT_WEAPON;
    	if(O instanceof Item) return OBJECT_ITEM;
    	return -1;
    }

	protected static final Object getClassSet(final String type) { return getClassSet(classCode(type));}
	protected static final Object getClassSet(final int code)
	{
		switch(code)
		{
		case 0: return c().races;
		case 1: return c().charClasses;
		case 2: return c().MOBs;
		case 3: return c().abilities;
		case 4: return c().locales;
		case 5: return c().exits;
		case 6: return c().items;
		case 7: return c().behaviors;
		case 8: return null;
		case 9: return c().weapons;
		case 10: return c().armor;
		case 11: return c().miscMagic;
		case 12: return c().areaTypes;
		case 13: return c().commands;
		case 14: return c().clanItems;
		case 15: return c().miscTech;
		case 16: return c().webMacros;
        case 17: return c().common;
        case 18: return c().libraries;
		}
		return null;
	}

	public static final int numPrototypes(final int type)
    {
		final Object o = getClassSet(type);
    	if(o instanceof Set) return ((Set)o).size();
    	if(o instanceof List) return ((List)o).size();
    	if(o instanceof Collection) return ((Collection)o).size();
    	if(o instanceof HashSet) return ((HashSet)o).size();
    	if(o instanceof Hashtable) return ((Hashtable)o).size();
    	if(o instanceof Vector) return ((Vector)o).size();
    	return 0;
    }
    
    public final static void unbumpCounter(final CMObject O, final int which)
    {
        if(KEEP_OBJECT_CACHE)
        {
            if(OBJECT_CACHE[which].containsKey(O)) // yes, if its in there, its bad
            {
                OBJECT_CACHE[which].remove(O);
                Log.errOut("bumped!",O.getClass().getName());
                return;
            }
        }
        OBJECT_DESTRUCTIONS[which]++;
    }
	public static final Enumeration<Race> 		races(){return c().races.elements();}
    public static final Enumeration<CMCommon> 	commonObjects(){return c().common.elements();}
	public static final Enumeration<CharClass> 	charClasses(){return c().charClasses.elements();}
	public static final Enumeration<MOB> 		mobTypes(){return c().MOBs.elements();}
    public static final Enumeration<CMLibrary> 	libraries(){return c().libraries.elements();}
	public static final Enumeration<Room> 		locales(){return c().locales.elements();}
	public static final Enumeration<Exit> 		exits(){return c().exits.elements();}
	public static final Enumeration<Behavior> 	behaviors(){return c().behaviors.elements();}
	public static final Enumeration<Item> 		basicItems(){return c().items.elements();}
	public static final Enumeration<Weapon> 	weapons(){return c().weapons.elements();}
	public static final Enumeration<Armor> 		armor(){return c().armor.elements();}
	public static final Enumeration<MiscMagic> 	miscMagic(){return c().miscMagic.elements();}
	public static final Enumeration<Electronics>miscTech(){return c().miscTech.elements();}
	public static final Enumeration<ClanItem> 	clanItems(){return c().clanItems.elements();}
	public static final Enumeration<Area> 		areaTypes(){return c().areaTypes.elements();}
	public static final Enumeration<Command> 	commands(){return c().commands.elements();}
	public static final Enumeration<Ability> 	abilities(){return c().abilities.elements();}
	public static final Enumeration<WebMacro> 	webmacros(){return c().webMacros.elements();}
	
	public static final Race 		randomRace(){return (Race)c().races.elementAt((int)Math.round(Math.floor(Math.random()*((double)c().races.size()))));}
	public static final CharClass 	randomCharClass(){return (CharClass)c().charClasses.elementAt((int)Math.round(Math.floor(Math.random()*((double)c().charClasses.size()))));}
	public static final Ability 	randomAbility(){ return (Ability)c().abilities.elementAt((int)Math.round(Math.floor(Math.random()*((double)c().abilities.size()))));}
    public static final Room 		getLocale(String calledThis){ return (Room)getNewGlobal(c().locales,calledThis); }
    public static final CMLibrary 	getLibrary(String calledThis) { return (CMLibrary)getGlobal(c().libraries,calledThis); }
    public static final Area 		anyOldArea(){return (Area)c().areaTypes.elementAt(0);}
    public static final Area 		getAreaType(String calledThis) { return (Area)getNewGlobal(c().areaTypes,calledThis); }
    public static final Exit 		getExit(String calledThis) { return (Exit)getNewGlobal(c().exits,calledThis);}
    public static final MOB 		getMOB(String calledThis) { return (MOB)getNewGlobal(c().MOBs,calledThis); }
    public static final Weapon 		getWeapon(String calledThis) { return (Weapon)getNewGlobal(c().weapons,calledThis); }
    public static final ClanItem 	getClanItem(String calledThis) { return (ClanItem)getNewGlobal(c().clanItems,calledThis); }
    public static final Item 		getMiscMagic(String calledThis) { return (Item)getNewGlobal(c().miscMagic,calledThis); }
    public static final Item 		getMiscTech(String calledThis) { return (Item)getNewGlobal(c().miscTech,calledThis);}
    public static final Armor 		getArmor(String calledThis) { return (Armor)getNewGlobal(c().armor,calledThis); }
    public static final Item 		getBasicItem(String calledThis) { return (Item)getNewGlobal(c().items,calledThis); }
    public static final Behavior 	getBehavior(String calledThis) { return (Behavior)getNewGlobal(c().behaviors,calledThis); }
    public static final Ability 	getAbility(String calledThis) { return (Ability)getNewGlobal(c().abilities,calledThis); }
    public static final CharClass 	getCharClass(String calledThis){ return (CharClass)getGlobal(c().charClasses,calledThis);}
    public static final CMCommon 	getCommon(String calledThis){return (CMCommon)getNewGlobal(c().common,calledThis);}
    public static final Command 	getCommand(String word){return (Command)getGlobal(c().commands,word);}
    public static final WebMacro 	getWebMacro(String macroName){return (WebMacro)c().webMacros.get(macroName);}
    public static final Race 		getRace(String calledThis){return (Race)getGlobal(c().races,calledThis);}


    public static final String getCounterReport()
    {
        StringBuffer str=new StringBuffer("");
        for(int i=0;i<OBJECT_TOTAL;i++)
            if(OBJECT_CREATIONS[i]>0)
                str.append(CMStrings.padRight(OBJECT_DESCS[i],12)+": Created: "+OBJECT_CREATIONS[i]+", Destroyed: "+OBJECT_DESTRUCTIONS[i]+", Remaining: "+(OBJECT_CREATIONS[i]-OBJECT_DESTRUCTIONS[i])+"\n\r");
        return str.toString();
    }

    public static final long numRemainingObjectCounts(final int type)
    {
    	return OBJECT_CREATIONS[type] - OBJECT_DESTRUCTIONS[type];
    }
    
    public static final int numPrototypes(final int[] types)
    {
    	int total=0;
    	for(int i=0;i<types.length;i++)
    		total+=numPrototypes(types[i]);
    	return total;
    }

	public static final void addAllItemClassNames(final Vector<String> V, final boolean NonArchon, 
												  final boolean NonGeneric, final boolean NonStandard)
	{
		V.addAll(getAllItemClassNames(basicItems(),NonArchon,NonGeneric,NonStandard));
		V.addAll(getAllItemClassNames(weapons(),NonArchon,NonGeneric,NonStandard));
		V.addAll(getAllItemClassNames(armor(),NonArchon,NonGeneric,NonStandard));
		V.addAll(getAllItemClassNames(miscMagic(),NonArchon,NonGeneric,NonStandard));
		V.addAll(getAllItemClassNames(miscTech(),NonArchon,NonGeneric,NonStandard));
		V.addAll(getAllItemClassNames(clanItems(),NonArchon,NonGeneric,NonStandard));
	}
	
    private static List<String> getAllItemClassNames(final Enumeration<? extends Item> i, 
    												 final boolean NonArchon, final boolean NonGeneric, final boolean NonStandard)
	{
    	final Vector<String> V=new Vector<String>();
		for(;i.hasMoreElements();)
		{
			Item I=(Item)i.nextElement();
			if(((!NonArchon)||(!(I instanceof ArchonOnly)))
			&&((!NonStandard)||(I.isGeneric()))
			&&((!NonGeneric)||(!I.isGeneric())))
				V.addElement(CMClass.classID(I));
		}
		return V;
	}

	public static Item getItem(final String calledThis)
	{
		Item thisItem=(Item)getNewGlobal(c().items,calledThis);
		if(thisItem==null) thisItem=(Item)getNewGlobal(c().armor,calledThis);
		if(thisItem==null) thisItem=(Item)getNewGlobal(c().weapons,calledThis);
		if(thisItem==null) thisItem=(Item)getNewGlobal(c().miscMagic,calledThis);
		if(thisItem==null) thisItem=(Item)getNewGlobal(c().clanItems,calledThis);
		if(thisItem==null) thisItem=(Item)getNewGlobal(c().miscTech,calledThis);
		return thisItem;
	}

    protected static Item sampleItem=null;
	public static final Item sampleItem(){
		if((sampleItem==null)&&(c().items.size()>0))
			sampleItem= (Item)((Item)c().items.firstElement()).copyOf();
		return sampleItem;
	}
    public static final Item sampleItem(final String itemID){
        Item thisItem=(Item)getNewGlobal(c().items,itemID);
        if(thisItem==null) thisItem=(Item)getGlobal(c().armor,itemID);
        if(thisItem==null) thisItem=(Item)getGlobal(c().weapons,itemID);
        if(thisItem==null) thisItem=(Item)getGlobal(c().miscMagic,itemID);
        if(thisItem==null) thisItem=(Item)getGlobal(c().clanItems,itemID);
        if(thisItem==null) thisItem=(Item)getGlobal(c().miscTech,itemID);
        return thisItem;
    }

    public static final MOB staticMOB(final String mobID)
    { return (MOB)CMClass.getGlobal(c().MOBs,mobID);}
    
    protected static MOB sampleMOB=null;
	public static final MOB sampleMOB()
	{
		if((sampleMOB==null)&&(c().MOBs.size()>0))
		{
			sampleMOB=(MOB)((MOB)c().MOBs.firstElement()).copyOf();
			sampleMOB.basePhyStats().setDisposition(PhyStats.IS_NOT_SEEN);
			sampleMOB.phyStats().setDisposition(PhyStats.IS_NOT_SEEN);
		}
		if(sampleMOB.location()==null)
			sampleMOB.setLocation(CMLib.map().getRandomRoom());
		return sampleMOB;
	}

	public static final Command findCommandByTrigger(final String word,
											   		 final boolean exactOnly)
	{
		Command C=(Command)c().commandWords.get(word.trim().toUpperCase());
		if((exactOnly)||(C!=null)) return C;
		String upword=word.toUpperCase();
		String key;
		for(Enumeration<String> e=c().commandWords.keys();e.hasMoreElements();)
		{
			key=(String)e.nextElement();
			if(key.toUpperCase().startsWith(upword))
				return (Command)c().commandWords.get(key);
		}
		return null;
	}

    protected final int totalLocalClasses(){
        return races.size()+charClasses.size()+MOBs.size()+abilities.size()+locales.size()+exits.size()
              +items.size()+behaviors.size()+weapons.size()+armor.size()+miscMagic.size()+clanItems.size()
              +miscTech.size()+areaTypes.size()+common.size()+libraries.size()+commands.size()
              +webMacros.size();
    }
    
    public static final int totalClasses(){ return c().totalLocalClasses();}

	public static final boolean delClass(final String type, final CMObject O)
	{
        if(classes.containsKey(O.getClass().getName()))
            classes.remove(O.getClass().getName());
        final Object set=getClassSet(type);
		if(set==null) return false;
		if(set instanceof List)
		{
			((List)set).remove(O);
			if(set instanceof XVector)
				((XVector)set).sort();
            if(set==c().commands) reloadCommandWords();
            //if(set==libraries) CMLib.registerLibraries(libraries.elements());
		}
		else
		if(set instanceof Hashtable)
			((Hashtable)set).remove(O.ID().trim());
		else
		if(set instanceof HashSet)
			((HashSet)set).remove(O);
		else
			return false;
		return true;
	}

	public static final boolean addClass(final String type, final CMObject O)
	{
		final Object set=getClassSet(type);
		if(set==null) return false;
		if(set instanceof List)
		{
			((List)set).add(O);
			if(set instanceof XVector)
				((XVector)set).sort();
            if(set==c().commands) reloadCommandWords();
            if(set==c().libraries) CMLib.registerLibraries(c().libraries.elements());
		}
		else
		if(set instanceof Hashtable)
			((Hashtable)set).put(O.ID().trim().toUpperCase(), O);
		else
		if(set instanceof HashSet)
			((HashSet)set).add(O);
		else
			return false;
		return true;
	}

	public final static int classCode(final String name)
	{
		for(int i=0;i<OBJECT_DESCS.length;i++)
		{
			if(OBJECT_DESCS[i].toUpperCase().startsWith(name.toUpperCase()))
				return i;
		}
		return -1;
	}

	public final static int classCode(final Object O)
	{
		for(int i=CMClass.OBJECT_ANCESTORS.length-1;i>=0;i--)
		{
			try{
				Class<?> ancestorCl = instance().loadClass(CMClass.OBJECT_ANCESTORS[i]);
				if(CMClass.checkAncestry(O.getClass(),ancestorCl))
					return i;
			}catch(Exception e){}
		}
		return -1;
	}

	
	public static final boolean loadClass(final String classType, final String path, final boolean quiet)
	{
        debugging=CMSecurity.isDebugging("CLASSLOADER");
        final Object set=getClassSet(classType);
		if(set==null) return false;

		if(!loadListToObj(set,path,OBJECT_ANCESTORS[classCode(classType)],quiet))
            return false;

        if(set instanceof List)
        {
			if(set instanceof XVector)
				((XVector)set).sort();
            if(set==c().commands) reloadCommandWords();
            if(set==c().libraries) CMLib.registerLibraries(c().libraries.elements());
        }
		return true;
	}

	public static final Object unsortedLoadClass(final String classType, String path, final boolean quiet)
	{
		if((path==null)||(path.length()==0))
			return null;
		try{
			String pathLess=path;
			if(pathLess.toUpperCase().endsWith(".CLASS"))
				pathLess=pathLess.substring(0,pathLess.length()-6);
			else
			if(pathLess.toUpperCase().endsWith(".JS"))
				pathLess=pathLess.substring(0,pathLess.length()-3);
			pathLess=pathLess.replace('/','.');
			pathLess=pathLess.replace('\\','.');
			if(classes.containsKey(pathLess))
				return (classes.get(pathLess)).newInstance();
		}catch(Exception e){}
		final Vector<Object> V=new Vector<Object>();
		if(classCode(classType)<0)
			return null;
		if((!path.toUpperCase().endsWith(".CLASS"))
		&&(!path.toUpperCase().endsWith(".JS")))
		{
			path=path.replace('.','/');
			path+=".class";
		}
		if(!loadListToObj(V,path,OBJECT_ANCESTORS[classCode(classType)],quiet))
			return null;
		if(V.size()==0) return null;
		return (Object)V.firstElement();
	}

	public final static boolean checkForCMClass(final String classType, final String path)
	{
		return unsortedLoadClass(classType,path,true)!=null;
	}

    public final static String ancestor(final String code)
    {
        int num=classCode(code);
        if((num>=0)&&(num<OBJECT_ANCESTORS.length))
            return OBJECT_ANCESTORS[num];
        return "";
    }
	
	public static final Object getClass(final String calledThis)
	{
		String shortThis=calledThis;
		final int x=shortThis.lastIndexOf('.');
		if(x>0) shortThis=shortThis.substring(x+1);
		Object set=null;
		Object thisItem=null;
		for(int i=0;i<CMClass.OBJECT_DESCS.length;i++)
		{
			set=getClassSet(i);
			if(set==null) continue;
			if(set instanceof List)
				thisItem=getGlobal((List)set,shortThis);
			else
			if(set instanceof Map)
				thisItem=getGlobal((Map)set,shortThis);
			if(thisItem!=null) return thisItem;
		}
        try{	return classes.get(calledThis).newInstance();}catch(Exception e){}
		return thisItem;
	}

	public static final Environmental getUnknown(final String calledThis)
	{
		Environmental thisItem=(Environmental)getNewGlobal(c().items,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().armor,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().weapons,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().miscMagic,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().miscTech,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().MOBs,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().abilities,calledThis);
		if(thisItem==null) thisItem=(Environmental)getNewGlobal(c().clanItems,calledThis);
		if((thisItem==null)&&(c().charClasses.size()>0)&&(calledThis.length()>0))
			Log.sysOut("CMClass","Unknown Unknown '"+calledThis+"'.");
		return thisItem;
	}

    public static final Race findRace(final String calledThis)
    {
    	final Race thisItem=getRace(calledThis);
        if(thisItem!=null) return thisItem;
        Race R;
        for(int i=0;i<c().races.size();i++)
        {
        	R=(Race)c().races.elementAt(i);
            if(R.name().equalsIgnoreCase(calledThis))
                return R;
        }
        return null;
    }

    public static final CharClass findCharClass(final String calledThis)
    {
    	final CharClass thisItem=getCharClass(calledThis);
        if(thisItem!=null) return thisItem;
        CharClass C;
        for(int i=0;i<c().charClasses.size();i++)
        {
            C=(CharClass)c().charClasses.elementAt(i);
            for(int n=0;n<C.nameSet().length;n++)
	            if(C.nameSet()[n].equalsIgnoreCase(calledThis))
	                return C;
        }
        return null;
    }

    public static final CMObject getNewGlobal(final List<? extends CMObject> list, final String ID)
    {
    	final CMObject O=(CMObject)getGlobal(list,ID);
        if(O!=null) return O.newInstance();
        return null;
    }

	public static final Object getGlobal(final List<? extends CMObject> list, final String ID)
	{
		if(list.size()==0) return null;
		int start=0;
		int end=list.size()-1;
		while(start<=end)
		{
			int mid=(end+start)/2;
			int comp=classID(list.get(mid)).compareToIgnoreCase(ID);
			if(comp==0)
				return list.get(mid);
			else
			if(comp>0)
				end=mid-1;
			else
				start=mid+1;

		}
		return null;
	}

	public static final Ability findAbility(final String calledThis)
	{
        return findAbility(calledThis,-1,-1,false);
	}

    public static final Ability findAbility(final String calledThis, final int ofClassDomain, final long ofFlags, final boolean exactOnly)
    {
    	final Vector ableV;
        Ability A;
        if((ofClassDomain>=0)||(ofFlags>=0))
        {
            ableV = new Vector();
            for(Enumeration<Ability> e=c().abilities.elements();e.hasMoreElements();)
            {
                A=(Ability)e.nextElement();
                if((ofClassDomain<0)
                ||((A.classificationCode() & Ability.ALL_ACODES)==ofClassDomain)
                ||((A.classificationCode() & Ability.ALL_DOMAINS)==ofClassDomain))
                {
                    if((ofFlags<0)
                    ||(CMath.bset(A.flags(),ofFlags)))
                        ableV.addElement(A);
                }
            }
        } 
        else
            ableV = c().abilities;
            
        A=(Ability)getGlobal(ableV,calledThis);
        if(A==null) A=(Ability)CMLib.english().fetchEnvironmental(ableV,calledThis,true);
        if((A==null)&&(!exactOnly)) A=(Ability)CMLib.english().fetchEnvironmental(ableV,calledThis,false);
        if(A!=null)A=(Ability)A.newInstance();
        return A;
    }

    public static final Behavior findBehavior(final String calledThis)
    {
        Behavior B=(Behavior)getGlobal(c().behaviors,calledThis);
        if(B==null) B=getBehaviorByName(calledThis,true);
        if(B==null) B=getBehaviorByName(calledThis,false);
        if(B!=null) B=(Behavior)B.copyOf();
        return B;
    }

    public static final Behavior getBehaviorByName(final String calledThis, final boolean exact)
    {
        if(calledThis==null) return null;
        Behavior B=null;
        for(Enumeration<Behavior> e=behaviors();e.hasMoreElements();)
        {
            B=(Behavior)e.nextElement();
            if(B.name().equalsIgnoreCase(calledThis))
                return (Behavior)B.copyOf();
        }
        if(exact) return null;
        for(Enumeration<Behavior> e=behaviors();e.hasMoreElements();)
        {
            B=(Behavior)e.nextElement();
            if(CMLib.english().containsString(B.name(),calledThis))
                return (Behavior)B.copyOf();
        }
        return null;
    }

	public static final Ability getAbilityByName(final String calledThis, final boolean exact)
	{
        if(calledThis==null) return null;
	    Ability A=null;
        for(Enumeration<Ability> e=abilities();e.hasMoreElements();)
        {
            A=(Ability)e.nextElement();
            if(A.name().equalsIgnoreCase(calledThis))
                return A;
        }
	    if(exact) return null;
        for(Enumeration<Ability> e=abilities();e.hasMoreElements();)
        {
            A=(Ability)e.nextElement();
            if(CMLib.english().containsString(A.name(),calledThis))
                return A;
        }
	    return null;
	}

	public static final Ability findAbility(final String calledThis, final CharStats charStats)
	{
		Ability A=null;
		final List<Ability> As=new LinkedList<Ability>();
		for(Enumeration<Ability> e=abilities();e.hasMoreElements();)
		{
			A=(Ability)e.nextElement();
			for(int c=0;c<charStats.numClasses();c++)
			{
				CharClass C=charStats.getMyClass(c);
				if(CMLib.ableMapper().getQualifyingLevel(C.ID(),true,A.ID())>=0)
				{	As.add(A); break;}
			}
		}
		A=(Ability)CMLib.english().fetchEnvironmental(As,calledThis,true);
		if(A==null) A=(Ability)CMLib.english().fetchEnvironmental(As,calledThis,false);
        if(A==null) A=(Ability)getGlobal(c().abilities,calledThis);
		if(A!=null)A=(Ability)A.newInstance();
		return A;
	}

	public static final Ability findAbility(final String calledThis, final MOB mob)
	{
		final List<Ability> As=new LinkedList<Ability>();
        Ability A=null;
        for(Enumeration<Ability> a=mob.abilities();a.hasMoreElements();)
        {
            A=a.nextElement();
		    if(A!=null) As.add(A);
		}
		A=(Ability)CMLib.english().fetchEnvironmental(As,calledThis,true);
		if(A==null)
			A=(Ability)CMLib.english().fetchEnvironmental(As,calledThis,false);
        if(A==null)
            A=(Ability)getGlobal(c().abilities,calledThis);
		if(A!=null)A=(Ability)A.newInstance();
		return A;
	}

    public static final CMObject getNewGlobal(final Map<String,? extends CMObject> list, final String ID)
    {
    	final CMObject O=(CMObject)getGlobal(list,ID);
        if(O!=null) return O.newInstance();
        return null;
    }

	public static final CMObject getGlobal(final Map<String,? extends CMObject> fromThese, final String calledThis)
	{
		CMObject o=fromThese.get(calledThis);
		if(o==null)
		{
			for(String s : fromThese.keySet())
			{
				o=fromThese.get(s);
				if(classID(o).equalsIgnoreCase(calledThis))
					return o;
			}
			return null;
		}
		return o;
	}

	public static final void addRace(final Race GR)
	{
		Race R;
		for(int i=0;i<c().races.size();i++)
		{
			R=(Race)c().races.elementAt(i);
			if(R.ID().compareToIgnoreCase(GR.ID())>=0)
			{
                if(R.ID().compareToIgnoreCase(GR.ID())==0)
                    c().races.setElementAt(GR,i);
                else
                    c().races.insertElementAt(GR,i);
				return;
			}
		}
        c().races.addElement(GR);
	}

	public static final void addCharClass(final CharClass CR)
	{
		for(int i=0;i<c().charClasses.size();i++)
		{
			CharClass C=(CharClass)c().charClasses.elementAt(i);
			if(C.ID().compareToIgnoreCase(CR.ID())>=0)
			{
                if(C.ID().compareToIgnoreCase(CR.ID())==0)
                    c().charClasses.setElementAt(CR,i);
                else
                    c().charClasses.insertElementAt(CR,i);
				return;
			}
		}
        c().charClasses.addElement(CR);
	}
	
	public static final void delCharClass(final CharClass C)
	{
        c().charClasses.removeElement(C);
	}
	
	public static final void delRace(final Race R)
	{
        c().races.removeElement(R);
	}

    public static final boolean returnMsg(final CMMsg msg)
    {
        synchronized(CMClass.MSGS_CACHE)
        {
            if(MSGS_CACHE.size()<10000)
            {
                MSGS_CACHE.add(msg);
                return true;
            }
        }
        return false;
    }

    public static final void sortEnvironmentalsByID(final List<Environmental> V) 
    {
    	final TreeMap<String,Environmental> hashed=new TreeMap<String,Environmental>();
        for(Environmental E : V)
            hashed.put(E.ID().toUpperCase(),E);
        V.clear();
        for(String key : hashed.keySet())
        	V.add(hashed.get(key));
    }

    public static final void sortEnvironmentalsByName(final List<Environmental> V) 
    {
        final TreeMap<String,LinkedList<Environmental>> nameHash=new TreeMap<String,LinkedList<Environmental>>();
        String name;
        LinkedList<Environmental> list;
        for(Environmental E : V)
        {
        	name=E.Name().toUpperCase();
        	list = nameHash.get(name);
        	if(list==null)
        	{
        		list=new LinkedList<Environmental>();
        		nameHash.put(name,list);
        	}
        	list.add(E);
        }
        V.clear();
        for(LinkedList<Environmental> l : nameHash.values())
        	V.addAll(l);
    }

    public final static CMMsg MsgFactory()
    {
        CMMsg msg=null;
        synchronized(CMClass.MSGS_CACHE)
        {
            if(MSGS_CACHE.size()==0)
            {
                if(MSGS_CACHE.size()==0)
                {
                    msg=(CMMsg)getCommon("DefaultMessage");
                    MSGS_CACHE.add(msg);
                }
            }
            msg=MSGS_CACHE.get(0);
            MSGS_CACHE.remove(0);
        }
        return msg;
    }

    public static final CMMsg getMsg(final MOB source, final int newAllCode, final String allMessage)
    { final CMMsg M=MsgFactory(); M.modify(source,newAllCode,allMessage); return M;}
    public static final CMMsg getMsg(final MOB source, final int newAllCode, final String allMessage, final int newValue)
    { final CMMsg M=MsgFactory(); M.modify(source,newAllCode,allMessage,newValue); return M;}
    public static final CMMsg getMsg(final MOB source, final Environmental target, final int newAllCode, final String allMessage)
    { final CMMsg M=MsgFactory(); M.modify(source,target,newAllCode,allMessage); return M;}
    public static final CMMsg getMsg(final MOB source, final Environmental target, final Environmental tool, final int newAllCode, final String allMessage)
    { final CMMsg M=MsgFactory(); M.modify(source,target,tool,newAllCode,allMessage); return M;}
    public static final CMMsg getMsg(final MOB source, final Environmental target, final Environmental tool, final int newSourceCode, final int newTargetCode, final int newOthersCode, final String Message)
    { final CMMsg M=MsgFactory(); M.modify(source,target,tool,newSourceCode,newTargetCode,newOthersCode,Message); return M;}
    public static final CMMsg getMsg(final MOB source, final Environmental target, final Environmental tool, final int newSourceCode, final String sourceMessage, final String targetMessage, final String othersMessage)
    { final CMMsg M=MsgFactory(); M.modify(source,target,tool,newSourceCode,sourceMessage,newSourceCode,targetMessage,newSourceCode,othersMessage); return M;}
    public static final CMMsg getMsg(final MOB source, final Environmental target, final Environmental tool, final int newSourceCode, final String sourceMessage, final int newTargetCode, final String targetMessage, final int newOthersCode, final String othersMessage)
    { final CMMsg M=MsgFactory(); M.modify(source,target,tool,newSourceCode,sourceMessage,newTargetCode,targetMessage,newOthersCode,othersMessage); return M;}


    public static final void shutdown() 
    {
        for(int c=0;c<clss.length;c++)
            if(clss[c]!=null)
                clss[c].unload();
    }
    
	public final void unload()
	{
        common.clear();
		races.clear();
		charClasses.clear();
		MOBs.clear();
		abilities.clear();
		locales.clear();
		exits.clear();
		items.clear();
		behaviors.clear();
		weapons.clear();
		armor.clear();
		miscMagic.clear();
		miscTech.clear();
		areaTypes.clear();
		clanItems.clear();
		commands.clear();
		webMacros.clear();
		commandWords.clear();
	}
	
    private final void initializeClassGroup(final List<? extends CMObject> V)
    { 
    	for(int v=0;v<V.size();v++) 
    		((CMObject)V.get(v)).initializeClass();
    }
    
    private final void initializeClassGroup(final Map<String,? extends CMObject> H)
    {
        for(Object o : H.keySet())
    		((CMObject)H.get(o)).initializeClass();
	}
    
    
	public final void intializeClasses()
    {
		final char tCode=Thread.currentThread().getThreadGroup().getName().charAt(0);
		final Vector privacyV=CMParms.parseCommas(CMProps.getVar(CMProps.SYSTEM_PRIVATERESOURCES).toUpperCase(),true);
        for(int o=0;o<OBJECT_DESCS.length;o++)
            if((tCode==MudHost.MAIN_HOST)||(privacyV.contains(OBJECT_DESCS[o])))
            {
            	Object set = CMClass.getClassSet(o); 
                if(set instanceof List)
                    initializeClassGroup((List)set);
                else
                if(set instanceof Hashtable)
                    initializeClassGroup((Map)set);
            }
    }
	
	public static Hashtable loadHashListToObj(final String filePath, String auxPath, final String ancester)
	{
		final Hashtable<String,Object> h=new Hashtable<String,Object>();
		int x=auxPath.indexOf(';');
		String path;
		while(x>=0)
		{
			path=auxPath.substring(0,x).trim();
			auxPath=auxPath.substring(x+1).trim();
			loadObjectListToObj(h,filePath,path,ancester);
			x=auxPath.indexOf(';');
		}
		loadObjectListToObj(h,filePath,auxPath,ancester);
		return h;
	}

	public static final XVector loadVectorListToObj(final String filePath, String auxPath, final String ancester)
	{
		final Vector v=new Vector();
		int x=auxPath.indexOf(';');
		String path;
		while(x>=0)
		{
			path=auxPath.substring(0,x).trim();
			auxPath=auxPath.substring(x+1).trim();
			loadObjectListToObj(v,filePath,path,ancester);
			x=auxPath.indexOf(';');
		}
		loadObjectListToObj(v,filePath,auxPath,ancester);
		return new XVector(new TreeSet(v));
	}
	
	public static final Vector loadClassList(final String filePath, String auxPath, final String subDir, final Class<?> ancestorC1, final boolean quiet)
	{
		final Vector v=new Vector();
		int x=auxPath.indexOf(';');
		while(x>=0)
		{
			String path=auxPath.substring(0,x).trim();
			auxPath=auxPath.substring(x+1).trim();
			if(path.equalsIgnoreCase("%default%"))
				loadListToObj(v,filePath, ancestorC1, quiet);
			else
				loadListToObj(v,path,ancestorC1, quiet);
			x=auxPath.indexOf(';');
		}
		if(auxPath.equalsIgnoreCase("%default%"))
			loadListToObj(v,filePath, ancestorC1, quiet);
		else
			loadListToObj(v,auxPath,ancestorC1, quiet);
		return v;
	}
	
	public static final boolean loadObjectListToObj(final Object o, final String filePath, final String path, final String ancester)
	{
		if(path.length()>0)
		{
			final boolean success;
			if(path.equalsIgnoreCase("%default%"))
				success=loadListToObj(o,filePath, ancester, false);
			else
				success=loadListToObj(o,path,ancester, false);
            return success;
		}
        return false;
	}
	
	public static final boolean loadListToObj(final Object toThis, final String filePath, final String ancestor, final boolean quiet)
	{
		final CMClass loader=new CMClass();
		Class<?> ancestorCl=null;
        if (ancestor != null && ancestor.length() != 0)
        {
            try
            {
                ancestorCl = loader.loadClass(ancestor);
            }
            catch (ClassNotFoundException e)
            {
                if(!quiet)
                    Log.sysOut("CMClass","WARNING: Couldn't load ancestor class: "+ancestor);
            }
        }
		return loadListToObj(toThis, filePath, ancestorCl, quiet);
	}
	
	public static final boolean loadListToObj(final Object toThis, final String filePath, final Class<?> ancestorCl, final boolean quiet)
	{
		final CMClass loader=new CMClass();
		final CMFile file=new CMFile(filePath,null,true);
		final Vector<String> fileList=new Vector<String>();
        if(file.canRead())
        {
            if(file.isDirectory())
            {
            	final CMFile[] list=file.listFiles();
                for(int l=0;l<list.length;l++)
                    if((list[l].getName().indexOf('$')<0)&&(list[l].getName().toUpperCase().endsWith(".CLASS")))
                        fileList.addElement(list[l].getVFSPathAndName());
                for(int l=0;l<list.length;l++)
                    if(list[l].getName().toUpperCase().endsWith(".JS"))
                        fileList.addElement(list[l].getVFSPathAndName());
            }
            else
            {
                fileList.addElement(file.getVFSPathAndName());
            }
        }
        else
        {
            if(!quiet)
                Log.errOut("CMClass","Unable to access path "+file.getVFSPathAndName());
            return false;
        }
        String item;
        for(int l=0;l<fileList.size();l++)
        {
            item=(String)fileList.elementAt(l);
            if(item.startsWith("/")) item=item.substring(1);
            try
            {
                Object O=null;
                String packageName=item.replace('/','.');
                if(packageName.toUpperCase().endsWith(".CLASS"))
                    packageName=packageName.substring(0,packageName.length()-6);
                final Class<?> C=loader.loadClass(packageName,true);
                if(C!=null)
                {
                    if(!checkAncestry(C,ancestorCl))
                    {
                        if(!quiet)
                            Log.sysOut("CMClass","WARNING: class failed ancestral check: "+packageName);
                    }
                    else
	                    O=C.newInstance();
                }
                if(O==null)
                {
                    if(!quiet)
                        Log.sysOut("CMClass","Unable to create class '"+packageName+"'");
                }
                else
                {
                    String itemName=O.getClass().getName();
                    final int x=itemName.lastIndexOf('.');
                    if(x>=0) itemName=itemName.substring(x+1);
                    if(toThis instanceof Hashtable)
                    {
                    	final Hashtable H=(Hashtable)toThis;
                        if(H.containsKey(itemName.trim().toUpperCase()))
                            H.remove(itemName.trim().toUpperCase());
                        H.put(itemName.trim().toUpperCase(),O);
                    }
                    else
                    if(toThis instanceof List)
                    {
                    	final List V=(List)toThis;
                        boolean doNotAdd=false;
                        for(int v=0;v<V.size();v++)
                            if(rawClassName(V.get(v)).equals(itemName))
                            {
                                V.set(v,O);
                                doNotAdd=true;
                                break;
                            }
                        if(!doNotAdd)
                            V.add(O);
                    }
                }
            }
            catch(Throwable e)
            {
                if(!quiet)
                    Log.errOut("CMClass",e);
                return false;
            }
        }
        return true;
    }

	public static final String rawClassName(final Object O)
	{
		if(O==null) return "";
		return rawClassName(O.getClass());
	}

	public static final String rawClassName(final Class<?> C)
	{
		if(C==null) return "";
		final String name=C.getName();
		final int lastDot=name.lastIndexOf('.');
		if(lastDot>=0)
			return name.substring(lastDot+1);
		return name;
	}

	public static final CMFile getClassDir(final Class<?> C) 
	{
		final URL location = C.getProtectionDomain().getCodeSource().getLocation();
		String loc;
		if(location == null) {
			return null;
		}
		
		loc=location.getPath();
		loc=loc.replace('/',File.separatorChar);
		String floc=new java.io.File(".").getAbsolutePath();
		if(floc.endsWith(".")) floc=floc.substring(0,floc.length()-1);
		if(floc.endsWith(File.separator)) floc=floc.substring(0,floc.length()-File.separator.length());
		int x=floc.indexOf(File.separator);
		if(x>=0)floc=floc.substring(File.separator.length());
		x=loc.indexOf(floc);
		loc=loc.substring(x+floc.length());
		loc=loc.replace(File.separatorChar,'/');
		return new CMFile("/"+loc,null,false);
	}

	public static final boolean checkAncestry(final Class<?> cl, final Class<?> ancestorCl)
	{
		if (cl == null) return false;
		if (cl.isPrimitive() || cl.isInterface()) return false;
		if ( Modifier.isAbstract( cl.getModifiers()) || !Modifier.isPublic( cl.getModifiers()) ) return false;
		if (ancestorCl == null) return true;
		return (ancestorCl.isAssignableFrom(cl)) ;
	}

    public static final String classPtrStr(final Object e)
    {
    	final String ptr=""+e;
    	final int x=ptr.lastIndexOf('@');
        if(x>0)return ptr.substring(x+1);
        return ptr;
    }

	public static final String classID(final Object e)
	{
		if(e!=null)
		{
			if(e instanceof CMObject)
				return ((CMObject)e).ID();
			else
			if(e instanceof Command)
				return rawClassName(e);
			else
				return rawClassName(e);
		}
		return "";
	}

    /**
     * This is a simple version for external clients since they
     * will always want the class resolved before it is returned
     * to them.
     */
    public final Class<?> loadClass(final String className) throws ClassNotFoundException 
    {
        return (loadClass(className, true));
    }

    public final Class<?> finishDefineClass(String className, final byte[] classData, final String overPackage, final boolean resolveIt)
        throws ClassFormatError
    {
        Class<?> result=null;
        if(overPackage!=null)
        {
            int x=className.lastIndexOf('.');
            if(x>=0)
                className=overPackage+className.substring(x);
            else
                className=overPackage+"."+className;
        }
        try{result=defineClass(className, classData, 0, classData.length);}
        catch(NoClassDefFoundError e)
        {
            if(e.getMessage().toLowerCase().indexOf("(wrong name:")>=0)
            {
                int x=className.lastIndexOf('.');
                if(x>=0)
                {
                    String notherName=className.substring(x+1);
                    result=defineClass(notherName, classData, 0, classData.length);
                }
                else
                    throw e;
            }
            else
                throw e;
        }
        if (result==null){throw new ClassFormatError();}
        if (resolveIt){resolveClass(result);}
        if(debugging) Log.debugOut("CMClass","Loaded: "+result.getName());
        classes.put(className, result);
        return result;
    }

    /**
     * This is the required version of loadClass<?> which is called
     * both from loadClass<?> above and from the internal function
     * FindClassFromClass.
     */
    public synchronized final Class<?> loadClass(String className, final boolean resolveIt)
        throws ClassNotFoundException
    {
        String pathName=null;
        if(className.endsWith(".class")) className=className.substring(0,className.length()-6);
        if(className.toUpperCase().endsWith(".JS"))
        {
            pathName=className.substring(0,className.length()-3).replace('.','/')+className.substring(className.length()-3);
            className=className.substring(0,className.length()-3);
        }
        else
            pathName=className.replace('.','/')+".class";
        Class<?> result = (Class<?>)classes.get(className);
        if (result!=null)
        {
            if(debugging) Log.debugOut("CMClass","Loaded: "+result.getName());
        	return result;
        }
        if((super.findLoadedClass(className)!=null)
        ||(className.indexOf("com.planet_ink.coffee_mud.")<0)
        ||(className.startsWith("com.planet_ink.coffee_mud.core."))
        ||(className.startsWith("com.planet_ink.coffee_mud.application."))
        ||(className.indexOf(".interfaces.")>=0))
        {
	        try{
	        	result=super.findSystemClass(className);
	        	if(result!=null)
	        	{
	                if(debugging) Log.debugOut("CMClass","Loaded: "+result.getName());
	        		return result;
	        	}
	        } catch(Throwable t){}
        }
        /* Try to load it from our repository */
        final CMFile CF=new CMFile(pathName,null,false);
        final byte[] classData=CF.raw();
        if((classData==null)||(classData.length==0))
        {
            throw new ClassNotFoundException("File "+pathName+" not readable!");
        }
        if(CF.getName().toUpperCase().endsWith(".JS"))
        {
        	final String name=CF.getName().substring(0,CF.getName().length()-3);
        	final StringBuffer str=CF.textVersion(classData);
            if((str==null)||(str.length()==0))
                throw new ClassNotFoundException("JavaScript file "+pathName+" not readable!");
            final List<String> V=Resources.getFileLineVector(str);
            Class<?> extendsClass=null;
            final Vector implementsClasses=new Vector();
            String overPackage=null;
            for(int v=0;v<V.size();v++)
            {
                if((extendsClass==null)&&((String)V.get(v)).trim().toUpperCase().startsWith("//EXTENDS "))
                {
                    String extendName=((String)V.get(v)).trim().substring(10).trim();
                    try{extendsClass=loadClass(extendName);}
                    catch(ClassNotFoundException e)
                    {
                        Log.errOut("CMClass","Could not load "+CF.getName()+" from "+className+" because "+extendName+" is an invalid extension.");
                        throw e;
                    }
                }
                if((overPackage==null)&&((String)V.get(v)).trim().toUpperCase().startsWith("//PACKAGE "))
                    overPackage=((String)V.get(v)).trim().substring(10).trim();
                if(((String)V.get(v)).toUpperCase().startsWith("//IMPLEMENTS "))
                {
                    String extendName=((String)V.get(v)).substring(13).trim();
                    Class<?> C=null;
                    try{C=loadClass(extendName);}catch(ClassNotFoundException e){continue;}
                    implementsClasses.addElement(C);
                }
            }
            final Context X=Context.enter();
            final JScriptLib jlib=new JScriptLib();
            X.initStandardObjects(jlib);
            jlib.defineFunctionProperties(JScriptLib.functions, JScriptLib.class, ScriptableObject.DONTENUM);
            final CompilerEnvirons ce = new CompilerEnvirons();
            ce.initFromContext(X);
            final ClassCompiler cc = new ClassCompiler(ce);
            if(extendsClass==null)
                Log.errOut("CMClass","Warning: "+CF.getVFSPathAndName()+" does not extend any class!");
            else
                cc.setTargetExtends(extendsClass);
            Class<?> mainClass=null;
            if(implementsClasses.size()>0)
            {
                Class[] CS=new Class[implementsClasses.size()];
                for(int i=0;i<implementsClasses.size();i++) CS[i]=(Class)implementsClasses.elementAt(i);
                cc.setTargetImplements(CS);
            }
            final Object[] objs = cc.compileToClassFiles(str.toString(), "script", 1, name);
            for (int i=0;i<objs.length;i+=2)
            {
                Class<?> C=finishDefineClass((String)objs[i],(byte[])objs[i+1],overPackage,resolveIt);
                if(mainClass==null) mainClass=C;
            }
            Context.exit();
            if((debugging)&&(mainClass!=null)) 
            	Log.debugOut("CMClass","Loaded: "+mainClass.getName());
            return mainClass;
        }
	    result=finishDefineClass(className,classData,null,resolveIt);
		return result;
    }

    protected static final void reloadCommandWords()
    {
    	c().commandWords.clear();
    	Command C;
    	String[] wordList;
        for(int c=0;c<c().commands.size();c++)
        {
            C=(Command)c().commands.elementAt(c);
            wordList=C.getAccessWords();
            if(wordList!=null)
                for(int w=0;w<wordList.length;w++)
                    c().commandWords.put(wordList[w].trim().toUpperCase(),C);
        }
    }

	public static final boolean loadClasses(final CMProps page)
    {
		CMClass c=c();
        if(c==null) c=new CMClass();
        final CMClass baseC=clss[MudHost.MAIN_HOST];
        final char tCode=Thread.currentThread().getThreadGroup().getName().charAt(0);
        // wait for baseC
        while((tCode!=MudHost.MAIN_HOST)&&(!classLoaderSync[0]))
        {try{Thread.sleep(500);}catch(Exception e){ break;}}
        
        final Vector privacyV=CMParms.parseCommas(CMProps.getVar(CMProps.SYSTEM_PRIVATERESOURCES).toUpperCase(),true);
        
        try
        {
        	final String prefix="com/planet_ink/coffee_mud/";
            debugging=CMSecurity.isDebugging("CLASSLOADER");
            
            c.libraries=loadVectorListToObj(prefix+"Libraries/",page.getStr("LIBRARY"),ancestor("LIBRARY"));
            if(c.libraries.size()==0) return false;
            CMLib.registerLibraries(c.libraries.elements());
            if(CMLib.unregistered().length()>0)
            {
                Log.errOut("CMClass","Fatal Error: libraries are unregistered: "+CMLib.unregistered().substring(0,CMLib.unregistered().length()-2));
                return false;
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("COMMON")))
                c.common=baseC.common;
            else
                c.common=loadHashListToObj(prefix+"Common/",page.getStr("COMMON"),ancestor("COMMON"));
            if(c.common.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("WEBMACROS")))
                c.webMacros=baseC.webMacros;
            else
            {
                c.webMacros=CMClass.loadHashListToObj(prefix+"WebMacros/", "%DEFAULT%",ancestor("WEBMACROS"));
                Log.sysOut(Thread.currentThread().getName(),"WebMacros loaded  : "+c.webMacros.size());
                for(Enumeration e=c.webMacros.keys();e.hasMoreElements();)
                {
                    String key=(String)e.nextElement();
                    if(key.length()>longestWebMacro)
                        longestWebMacro=key.length();
                }
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("RACE")))
                c.races=baseC.races;
            else
            {
                c.races=loadVectorListToObj(prefix+"Races/",page.getStr("RACES"),ancestor("RACE"));
                Log.sysOut(Thread.currentThread().getName(),"Races loaded      : "+c.races.size());
            }
            if(c.races.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("CHARCLASS")))
                c.charClasses=baseC.charClasses;
            else
            {
                c.charClasses=loadVectorListToObj(prefix+"CharClasses/",page.getStr("CHARCLASSES"),ancestor("CHARCLASS"));
                Log.sysOut(Thread.currentThread().getName(),"Classes loaded    : "+c.charClasses.size());
            }
            if(c.charClasses.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("MOB")))
                c.MOBs=baseC.MOBs;
            else
            {
                c.MOBs=loadVectorListToObj(prefix+"MOBS/",page.getStr("MOBS"),ancestor("MOB"));
                Log.sysOut(Thread.currentThread().getName(),"MOB Types loaded  : "+c.MOBs.size());
            }
            if(c.MOBs.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("EXIT")))
                c.exits=baseC.exits;
            else
            {
                c.exits=loadVectorListToObj(prefix+"Exits/",page.getStr("EXITS"),ancestor("EXIT"));
                Log.sysOut(Thread.currentThread().getName(),"Exit Types loaded : "+c.exits.size());
            }
            if(c.exits.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("AREA")))
                c.areaTypes=baseC.areaTypes;
            else
            {
                c.areaTypes=loadVectorListToObj(prefix+"Areas/",page.getStr("AREAS"),ancestor("AREA"));
                Log.sysOut(Thread.currentThread().getName(),"Area Types loaded : "+c.areaTypes.size());
            }
            if(c.areaTypes.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("LOCALE")))
                c.locales=baseC.locales;
            else
            {
                c.locales=loadVectorListToObj(prefix+"Locales/",page.getStr("LOCALES"),ancestor("LOCALE"));
                Log.sysOut(Thread.currentThread().getName(),"Locales loaded    : "+c.locales.size());
            }
            if(c.locales.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("ABILITY")))
                c.abilities=baseC.abilities;
            else
            {
                c.abilities=loadVectorListToObj(prefix+"Abilities/",page.getStr("ABILITIES"),ancestor("ABILITY"));
                if(c.abilities.size()==0) return false;
                if((page.getStr("ABILITIES")!=null)
                &&(page.getStr("ABILITIES").toUpperCase().indexOf("%DEFAULT%")>=0))
                {
                    Vector tempV;
                    int size=0;
                    tempV=loadVectorListToObj(prefix+"Abilities/Fighter/","%DEFAULT%",ancestor("ABILITY"));
                    size=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Ranger/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Paladin/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    size+=tempV.size();
                    if(size>0) Log.sysOut(Thread.currentThread().getName(),"Fighter Skills    : "+size);
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Druid/","%DEFAULT%",ancestor("ABILITY"));
                    if(tempV.size()>0) Log.sysOut(Thread.currentThread().getName(),"Chants loaded     : "+tempV.size());
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Languages/","%DEFAULT%",ancestor("ABILITY"));
                    if(tempV.size()>0) Log.sysOut(Thread.currentThread().getName(),"Languages loaded  : "+tempV.size());
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Properties/","%DEFAULT%",ancestor("ABILITY"));
                    size=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Diseases/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Poisons/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Misc/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    Log.sysOut(Thread.currentThread().getName(),"Properties loaded : "+size);
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Prayers/","%DEFAULT%",ancestor("ABILITY"));
                    Log.sysOut(Thread.currentThread().getName(),"Prayers loaded    : "+tempV.size());
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Archon/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Skills/","%DEFAULT%",ancestor("ABILITY"));
                    size=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Thief/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Common/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Specializations/","%DEFAULT%",ancestor("ABILITY"));
                    size+=tempV.size();
                    c.abilities.addAll(tempV);
                    if(size>0) Log.sysOut(Thread.currentThread().getName(),"Skills loaded     : "+size);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Songs/","%DEFAULT%",ancestor("ABILITY"));
                    if(tempV.size()>0) Log.sysOut(Thread.currentThread().getName(),"Songs loaded      : "+tempV.size());
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Spells/","%DEFAULT%",ancestor("ABILITY"));
                    if(tempV.size()>0) Log.sysOut(Thread.currentThread().getName(),"Spells loaded     : "+tempV.size());
                    c.abilities.addAll(tempV);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/SuperPowers/","%DEFAULT%",ancestor("ABILITY"));
                    size=tempV.size();
                    c.abilities.addAll(tempV);
                    if(size>0) Log.sysOut(Thread.currentThread().getName(),"Heroics loaded    : "+size);
    
                    tempV=loadVectorListToObj(prefix+"Abilities/Traps/","%DEFAULT%",ancestor("ABILITY"));
                    if(tempV.size()>0) Log.sysOut(Thread.currentThread().getName(),"Traps loaded      : "+tempV.size());
                    c.abilities.addAll(tempV);
                    
                    c.abilities.sort();

                    CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: reading genAbilities");
                    final List<DatabaseEngine.AckRecord> genAbilities=CMLib.database().DBReadAbilities();
                    if(genAbilities.size()>0)
                    {
                        int loaded=0;
                        for(int r=0;r<genAbilities.size();r++)
                        {
                            Ability A=(Ability)(CMClass.getAbility("GenAbility").copyOf());
                            A.setStat("ALLXML",genAbilities.get(r).data);
                            if(!A.ID().equals("GenAbility"))
                            {
                                c.abilities.addElement(A);
                                loaded++;
                            }
                        }
                        if(loaded>0)
                        {
                            Log.sysOut(Thread.currentThread().getName(),"GenAbles loaded   : "+loaded);
                            c.abilities.sort();
                        }
                    }
                }
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("ITEM")))
                c.items=baseC.items;
            else
            {
                c.items=loadVectorListToObj(prefix+"Items/Basic/",page.getStr("ITEMS"),ancestor("ITEM"));
                if(c.items.size()>0) Log.sysOut(Thread.currentThread().getName(),"Basic Items loaded: "+c.items.size());
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("WEAPON")))
                c.weapons=baseC.weapons;
            else
            {
                c.weapons=loadVectorListToObj(prefix+"Items/Weapons/",page.getStr("WEAPONS"),ancestor("WEAPON"));
                if(c.weapons.size()>0) Log.sysOut(Thread.currentThread().getName(),"Weapons loaded    : "+c.weapons.size());
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("ARMOR")))
                c.armor=baseC.armor;
            else
            {
                c.armor=loadVectorListToObj(prefix+"Items/Armor/",page.getStr("ARMOR"),ancestor("ARMOR"));
                if(c.armor.size()>0) Log.sysOut(Thread.currentThread().getName(),"Armor loaded      : "+c.armor.size());
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("MISCMAGIC")))
                c.miscMagic=baseC.miscMagic;
            else
            {
                c.miscMagic=loadVectorListToObj(prefix+"Items/MiscMagic/",page.getStr("MISCMAGIC"),ancestor("MISCMAGIC"));
                if(c.miscMagic.size()>0) Log.sysOut(Thread.currentThread().getName(),"Magic Items loaded: "+c.miscMagic.size());
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("CLANITEMS")))
                c.clanItems=baseC.clanItems;
            else
            {
                c.clanItems=loadVectorListToObj(prefix+"Items/ClanItems/",page.getStr("CLANITEMS"),ancestor("CLANITEMS"));
                if(c.clanItems.size()>0) Log.sysOut(Thread.currentThread().getName(),"Clan Items loaded : "+c.clanItems.size());
            }

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("MISCTECH")))
                c.miscTech=baseC.miscTech;
            else
            {
                c.miscTech=loadVectorListToObj(prefix+"Items/MiscTech/",page.getStr("MISCTECH"),ancestor("MISCTECH"));
                if(c.miscTech.size()>0) Log.sysOut(Thread.currentThread().getName(),"Electronics loaded: "+c.miscTech.size());
                Vector tempV=loadVectorListToObj(prefix+"Items/Software/",page.getStr("SOFTWARE"),"com.planet_ink.coffee_mud.Items.interfaces.Software");
                if(tempV.size()>0) c.miscTech.addAll(tempV);

                c.miscTech.sort();
            }

            if((c.items.size()+c.weapons.size()+c.armor.size()+c.miscTech.size()+c.miscMagic.size()+c.clanItems.size())==0)
                return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("BEHAVIOR")))
                c.behaviors=baseC.behaviors;
            else
            {
                c.behaviors=loadVectorListToObj(prefix+"Behaviors/",page.getStr("BEHAVIORS"),ancestor("BEHAVIOR"));
                Log.sysOut(Thread.currentThread().getName(),"Behaviors loaded  : "+c.behaviors.size());
            }
            if(c.behaviors.size()==0) return false;

            if((tCode!=MudHost.MAIN_HOST)&&(!privacyV.contains("COMMAND")))
            {
                c.commands=baseC.commands;
                c.commandWords=baseC.commandWords;
            }
            else
            {
                c.commands=loadVectorListToObj(prefix+"Commands/",page.getStr("COMMANDS"),ancestor("COMMAND"));
                Log.sysOut(Thread.currentThread().getName(),"Commands loaded   : "+c.commands.size());
            }
            if(c.commands.size()==0) return false;
        }
        catch(Throwable t)
        {
            t.printStackTrace();
            return false;
        }

        reloadCommandWords();

        // misc startup stuff
        if((tCode==MudHost.MAIN_HOST)||(privacyV.contains("CHARCLASS")))
            for(int i=0;i<c.charClasses.size();i++)
            {
                CharClass C=(CharClass)c.charClasses.elementAt(i);
                C.copyOf();
            }
        if((tCode==MudHost.MAIN_HOST)||(privacyV.contains("RACE")))
        {
            for(int r=0;r<c.races.size();r++)
            {
                Race R=(Race)c.races.elementAt(r);
                R.copyOf();
            }
            CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: reading genRaces");
            final List<DatabaseEngine.AckRecord> genRaces=CMLib.database().DBReadRaces();
            if(genRaces.size()>0)
            {
                int loaded=0;
                for(int r=0;r<genRaces.size();r++)
                {
                    Race GR=(Race)getRace("GenRace").copyOf();
                    GR.setRacialParms(genRaces.get(r).data);
                    if(!GR.ID().equals("GenRace"))
                    {
                        addRace(GR);
                        loaded++;
                    }
                }
                if(loaded>0)
                    Log.sysOut(Thread.currentThread().getName(),"GenRaces loaded   : "+loaded);
            }
        }
        if((tCode==MudHost.MAIN_HOST)||(privacyV.contains("CHARCLASS")))
        {
            CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: reading genClasses");
            final List<DatabaseEngine.AckRecord> genClasses=CMLib.database().DBReadClasses();
            if(genClasses.size()>0)
            {
                int loaded=0;
                for(int r=0;r<genClasses.size();r++)
                {
                    CharClass CR=(CharClass)(CMClass.getCharClass("GenCharClass").copyOf());
                    CR.setClassParms(genClasses.get(r).data);
                    if(!CR.ID().equals("GenCharClass"))
                    {
                        addCharClass(CR);
                        loaded++;
                    }
                }
                if(loaded>0)
                    Log.sysOut(Thread.currentThread().getName(),"GenClasses loaded : "+loaded);
            }
        }
        CMProps.setUpLowVar(CMProps.SYSTEM_MUDSTATUS,"Booting: initializing classes");
        c.intializeClasses();
        if((tCode==MudHost.MAIN_HOST)||(privacyV.contains("EXPERTISES")))
        {
            CMLib.expertises().recompileExpertises();
            Log.sysOut(Thread.currentThread().getName(),"Expertises defined: "+CMLib.expertises().numExpertises());
        }
        if(tCode==MudHost.MAIN_HOST)
            classLoaderSync[0]=true;
        return true;
    }
    
    protected static final class JScriptLib extends ScriptableObject
    {
        public String getClassName(){ return "JScriptLib";}
        static final long serialVersionUID=47;
        public static String[] functions = {"toJavaString"};
        public String toJavaString(Object O){return Context.toString(O);}
    }
}
