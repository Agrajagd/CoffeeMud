package com.planet_ink.coffee_mud.Common.interfaces;
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
import com.planet_ink.coffee_mud.Libraries.interfaces.XMLLibrary;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.Vector;

/* 
   Copyright 2000-2008 Bo Zimmerman

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
/**
 * A CoffeeShop is an object for storing the inventory of a shopkeeper, banker,
 * auctionhouse, merchant, or other object that implements the ShopKeeper interface
 * for the purpose of selling goods and services.
 * 
 * ShopKeepers maintain two types of inventory, the base inventory, and the stock
 * inventory. The stock or store inventory is the list of items the shopkeeper
 * currently has for sale, the amounts, base prices, etc. 
 * The base inventory is used only for shopkeepers who only buy things like
 * they originally had in stock, and so the base inventory is always populated with
 * a single copy of the original store inventory, to be used as a base of comparison
 * for situations where the stock is empty, but someone is wanting to sell.
 * 
 * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper
 * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
 * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#DEAL_INVENTORYONLY
 */
public interface CoffeeShop extends CMCommon
{
    /**
     * Returns whether an item sufficiently like the given item originally
     * existed in this shops inventory when it was created.  Applies only
     * to shops where their whatIsSold method returns ONLY_INVENTORY
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#DEAL_INVENTORYONLY
     * @param thisThang the thing to compare against the base inventory
     * @return whether the item, or one just like it, is in the base inventory
     */
    public boolean inBaseInventory(Environmental thisThang);
    
    /**
     * Adds a new item to the store inventory.  Use this method when an item is sold
     * to the store, as pricing and other information will have to be derived.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#addStoreInventory(Environmental, int, int, ShopKeeper)
     * @param thisThang the thing to sell
     * @param shop the shop that's selling it
     * @return the core store inventory item added
     */
    public Environmental addStoreInventory(Environmental thisThang, ShopKeeper shop);
    
    /**
     * Returns the number of items in the stores base inventory.  Only really useful
     * for historical reasons, or if the shop sells inventory only.
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#DEAL_INVENTORYONLY
     * @return the number of items in the base inventory
     */
    public int baseStockSize();
    
    /**
     * Returns the number of items this shop currently has for sale.  Does not
     * take number of duplicates into account.  For that call totalStockSize
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#totalStockSizeIncludingDuplicates()
     * @return the number of items for sale.
     */
    public int totalStockSize();
    
    /**
     * Returns a Vector of all the Environmental objects this shop has for sale.
     * Will only return one of each item, even if multiple are available.
     * @return a Vector of objects for sale.
     */
    public Vector<Environmental> getStoreInventory();
    
    /**
     * Returns a Vector of all the Environmental objects this shop has in its base
     * inventory.  Only useful for historical reasons, or if the shop sells inventory
     * only.
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#DEAL_INVENTORYONLY
     * @return a Vector of objects in base inventory
     */
    public Vector<Environmental> getBaseInventory();
    
    /**
     * Clears both the base and stock/store inventories.
     */
    public void emptyAllShelves();
    
    /**
     * Adds a new item to the store inventory so the shopkeeper can sell it.  All items
     * added go cumulatively into the store inventory, and one copy is kept in the 
     * base inventory for historical reasons.  The method is called when multiple items
     * need to be added, or if the price is available.  This method is usually used to
     * build an original shop inventory.
     * @param thisThang the item/mob/ability to sell 
     * @param number the number of items to sell
     * @param price the price of the item (in base currency) or -1 to have it determined
     * @param shop the shop selling the item
     * @return the actual object stored in the inventory
     */
    public Environmental addStoreInventory(Environmental thisThang, int number, int price, ShopKeeper shop);
    
    /**
     * Total weight, in pounds, of all items in the store inventory, taking number in
     * stock into account.
     * @return the total weight in pounds
     */
    public int totalStockWeight();
    
    /**
     * The number of items in the store inventory, taking number in stock into account.
     * Call this method to see how crowded the shop really is, as opposed to totalStockSize.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#totalStockSize()
     * @return the total number of all items in stock
     */
    public int totalStockSizeIncludingDuplicates();
    
    /**
     * Removes all items like the given item from the base and store inventory.
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
     * @param thisThang the item like which to remove
     * @param whatISell reference to what kind of stuff the store sells
     */
    public void delAllStoreInventory(Environmental thisThang, int whatISell);

    /**
     * Returns whether an item with the given name is presently in this stores
     * stock inventory, and available for sale.
     * @see com.planet_ink.coffee_mud.core.interfaces.ShopKeeper#whatIsSold()
     * @param name the name of the item to search for
     * @param mob the mob who is interested (stock can differ depending on customer)
     * @param whatISell reference to what kind of stuff the store sells
     * @param startRoom the shops start room, for determining jurisdiction
     * @return whether the item is available
     */
    public boolean doIHaveThisInStock(String name, MOB mob, int whatISell, Room startRoom);
    
    /**
     * Returns the base stock price (not the final price by any means) that the shop
     * will use as a foundation for determining the given items price.  -1 would mean
     * that the shopkeeper uses the valuation of the item as a basis, whereas another
     * value is in base gold.  Best to get likeThis item from the getStoreInventory()
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @param likeThis the item like which to compare
     * @return the stock price of the item given.
     */
    public int stockPrice(Environmental likeThis);
    
    /**
     * Returns the number of items like the one given that the shopkeeper presently
     * has in stock and available for sale.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @param likeThis the item like which to compare
     * @return the number currently in stock.
     */
    public int numberInStock(Environmental likeThis);
    
    /**
     * Searches this shops stock of items for sale for one matching the given name.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @param name the name of the item to search for
     * @param mob the mob who is interested (stock can differ depending on customer)
     * @param whatISell reference to what kind of stuff the store sells
     * @param startRoom the shops start room, for determining jurisdiction
     * @return the available item, if found
     */
    public Environmental getStock(String name, MOB mob, int whatISell, Room startRoom);
    
    /**
     * Searches this shops stock of items for sale for one matching the given name.
     * If one is found, it copies the item, removes one from the available stock, and
     * returns the copy.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @param name the name of the item to search for
     * @param mob the mob who is interested (stock can differ depending on customer)
     * @param whatISell reference to what kind of stuff the store sells
     * @param startRoom the shops start room, for determining jurisdiction
     * @return the available item, if found
     */
    public Environmental removeStock(String name, MOB mob, int whatISell, Room startRoom);
    
    /**
     * Searches this shops stock of items for sale for one matching the given name.
     * If one is found, it copies the item, removes one from the available stock, and
     * prepares it for sale by adding it to a Vector along with any necessary accessories,
     * such as necessary keys, or if a container, any contents of the container.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @param named the name of the item to search for
     * @param mob the mob who is interested (stock can differ depending on customer)
     * @param whatISell reference to what kind of stuff the store sells
     * @param startRoom the shops start room, for determining jurisdiction
     * @return the available items, if found, as a Vector of Environmental objects
     */
    public Vector<Environmental> removeSellableProduct(String named, MOB mob, int whatISell, Room startRoom);
    
    /**
     * Generates an XML document of all available shop inventory, prices, and availability.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#getStoreInventory()
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#buildShopFromXML(String, ShopKeeper)
     * @param shop the shopkeeper shop
     * @return an XML document.
     */
    public String makeXML(ShopKeeper shop);
    
    /**
     * Repopulates this shop inventory from a given xml document, restoring store inventory,
     * base inventory, prices, and availability.
     * @see com.planet_ink.coffee_mud.Common.interfaces.CoffeeShop#makeXML(ShopKeeper)
     * @param text the xml document to restore from
     * @param shop the shopkeeper shop
     */
    public void buildShopFromXML(String text, ShopKeeper shop);
}
