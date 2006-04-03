/*
 * Copyright (c) 1997-2005 by media style GmbH
 * 
 * $Source: /cvs/asp-search/src/java/com/ms/aspsearch/PermissionDeniedException.java,v $
 */

package de.ingrid.ibus.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.ingrid.ibus.ResultSet;
import de.ingrid.ibus.registry.Registry;
import de.ingrid.utils.IPlug;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.query.IngridQuery;

/**
 * A thread for one query to one IPlug. Created on 24.10.2005
 */
public class PlugQueryRequest extends Thread {

	private final static Log fLog = LogFactory.getLog(PlugQueryRequest.class);

	private IngridQuery fQuery;

	private int fLength;

	private ResultSet fResultSet;

	private int fStart;

	private IPlug fIPlug;

	private String fPlugId;

	private Registry fRegestry;

	/**
	 * @param proxyFactory
	 * @param plugDescription
	 * @param query
	 * @param start
	 * @param length
	 * @param plug
	 * @throws Exception
	 */
	public PlugQueryRequest(IPlug plug, Registry registry, String plugId,
			ResultSet resultSet, IngridQuery query, int start, int length)
			throws Exception {
		this.fIPlug = plug;
		this.fRegestry = registry;
		this.fPlugId = plugId;
		this.fResultSet = resultSet;
		this.fQuery = query;
		this.fStart = start;
		this.fLength = length;

	}

	/**
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		
		try {
			if (fLog.isDebugEnabled()) {
				fLog.debug("Search in IPlug " + this.fPlugId + " ...");
			}
			IngridHits hits = this.fIPlug.search(this.fQuery, this.fStart,
					this.fLength);

			if (fLog.isDebugEnabled()) {
				fLog.debug("adding results from: " + fPlugId + " size: "
						+ hits.length());
			}
			this.fResultSet.add(hits);

		} catch (Exception e) {
			fLog.error(
					"(REMOVING IPLUG!) Could not retrieve query result from IPlug: "
							+ this.fPlugId, e);
			fRegestry.removePlugFromCache(fPlugId);

		} finally {
			if ( this.fResultSet !=null ) {
                synchronized (fResultSet) {
                    this.fResultSet.resultsAdded();    
                }
			} else {
				fLog.error("No ResultSet set where IPlug " + this.fPlugId
						+ " can sent its completion.");
			}
		}
	}

	/**
	 * @return The IPlug itself.
	 */
	public IPlug getIPlug() {
		return this.fIPlug;
	}
}
