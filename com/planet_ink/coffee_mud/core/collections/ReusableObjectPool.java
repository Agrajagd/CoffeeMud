package com.planet_ink.coffee_mud.core.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.planet_ink.coffee_mud.core.CMParms;
import com.planet_ink.coffee_mud.core.CMath;
import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.core.interfaces.CMObject;
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
public class ReusableObjectPool<T extends CMObject> 
{
	private final NotifyingCMObjectVector<T>   			masterList;
	private final Stack<NotifyingCMObjectVector<T>>		masterPool = new Stack<NotifyingCMObjectVector<T>>();
	private volatile int								outstanding= 0;
	private volatile int								requests   = 0;
	private volatile int								returns    = 0;
	private final    int								minEntries;
	private final    Object								sync	   = new Object();
	private final    PoolFixer							fixer	   = new PoolFixer();

	private class PoolFixer implements Runnable
	{
		private volatile boolean running = false;
		public void run()
		{
			if(running) return;
			try
			{
				running = true;
				synchronized(this)
				{
					while(masterPool.size() < minEntries)
					{
						if(masterPool.size() < minEntries)
						{
							System.gc();
							try{Thread.sleep(100);}catch(Exception e){}
							synchronized(sync)
							{
								masterPool.add(makeNewEntry());
							}
							Runtime.getRuntime().runFinalization(); 
							try{Thread.sleep(100);}catch(Exception e){}
							System.gc();
						}
					}
				}
			}
			finally
			{
				running = false;
			}
		}
	}
	
	private class NotifyingCMObjectVector<K extends CMObject> extends Vector<T>
	{
		private static final long serialVersionUID = 1L;
		
		public NotifyingCMObjectVector(final List<T> V)
		{
			super(V);
		}
		public NotifyingCMObjectVector(int size)
		{
			super(size);
		}
		protected void finalize() throws Throwable
		{
			final NotifyingCMObjectVector<T> V = new NotifyingCMObjectVector<T>(this);
			synchronized(sync)
			{
				returns++;
		    	masterPool.push(V);
			}
			super.finalize();
		}
	}
	
	public ReusableObjectPool(final List<T> initialEntry, final int minEntries)
	{
		if(initialEntry.size()==0)
			this.masterList = new NotifyingCMObjectVector<T>(new ReadOnlyList<T>(initialEntry));
		else
			this.masterList = new NotifyingCMObjectVector<T>(initialEntry);
		this.minEntries = (minEntries<2)?2:minEntries;
		for(int i=0;i<minEntries;i++)
			masterPool.add(makeNewEntry());
	}
	
	public int getMasterPoolSize() { return masterPool.size();}
	
	@SuppressWarnings("unchecked")
	private final NotifyingCMObjectVector<T> makeNewEntry()
	{
		final NotifyingCMObjectVector<T> myList=new NotifyingCMObjectVector<T>(masterList.size());
		for(final T o : masterList)
			myList.add((T)o.copyOf());
		outstanding++;
		if(outstanding == minEntries * 1000)
			Log.errOut("ReuseOP","Reusable Object Pool pass all reason: "+CMParms.toStringList(masterList));
		return myList;
	}
	
	public final List<T> get()
	{
		if(masterList.size()==0)
			return masterList;
		requests++;
		synchronized(sync)
		{
			if(!masterPool.isEmpty())
			{
				final NotifyingCMObjectVector<T> myList=masterPool.pop();
				if(myList != null)
				{
					return myList;
				}
			}
			new Thread(fixer).start();
			return makeNewEntry();
		}
	}
}
