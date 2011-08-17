/*
 * Copyright (c) 1997-2005 by media style GmbH
 * 
 * $Source: /cvs/asp-search/src/java/com/ms/aspsearch/PermissionDeniedException.java,v $
 */

package de.ingrid.ibus.net;

import java.io.IOException;

import net.weta.components.communication.tcp.TimeoutException;

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
public class PlugQueryRequest implements Runnable {

    private final static Log fLog = LogFactory.getLog(PlugQueryRequest.class);

    private IngridQuery fQuery;

    private int fLength;

    private ResultSet fResultSet;

    private int fStart;

    private IPlug fIPlug;

    private String fPlugId;

    private Registry fRegistry;

    /**
     * Creates a request to an IPlug. This query is bound to a result set and synchronizes on it.
     * 
     * @param query
     *            The query to send.
     * @param start
     *            The positon to start the search on the IPlug.
     * @param length
     *            The length of the result from the IPlug.
     * @param plug
     *            The IPlug instance.
     * @param registry
     *            The registry where all IPlugs are registered.
     * @param plugId
     *            The id of the plug.
     * @param resultSet
     *            The ResultSet where the results should be added to.
     * @throws Exception
     */
    public PlugQueryRequest(IPlug plug, Registry registry, String plugId, ResultSet resultSet, IngridQuery query,
            int start, int length) throws Exception {
        this.fIPlug = plug;
        this.fRegistry = registry;
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
        long time = 0;
        try {
            if (fLog.isDebugEnabled()) {
                fLog.debug("Search in IPlug " + this.fPlugId + " ...");
                time = System.currentTimeMillis();
            }
            IngridHits hits = this.fIPlug.search(this.fQuery, this.fStart, this.fLength);
            if (hits.getPlugId() == null) {
                if (fLog.isDebugEnabled()) {
                    fLog.debug(("result from '".concat(this.fPlugId)).concat("' contains no plugId"));
                }
                hits.setPlugId(this.fPlugId);
            }
            if (fLog.isDebugEnabled()) {
                fLog.debug("adding results from: " + this.fPlugId + " size: " + hits.length() + " time: "
                        + (System.currentTimeMillis() - time) + " ms");
            }
            synchronized (this.fResultSet) {
                this.fResultSet.add(hits);
            }
        } catch (TimeoutException e) {
            if (fLog.isWarnEnabled()) {
                fLog
                        .warn("IPlug: "
                                + this.fPlugId
                                + " sent timeout. Set the timeout for the search lower than the communication timeout for the IPlug or vice versa. TimeoutException: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            if (fLog.isErrorEnabled()) {
                fLog.error("(REMOVING IPLUG!) Interrupted query result retrieval: " + this.fPlugId);
            }
            this.fRegistry.removePlug(this.fPlugId);
        } catch (IOException e) {
            if (fLog.isErrorEnabled()) {
                fLog.error("(REMOVING IPLUG!) Could not retrieve query result from IPlug: '" + this.fPlugId + "' - "
                        + e.getMessage());
            }
            this.fRegistry.removePlug(this.fPlugId);
        } catch (Exception e) {
            if (fLog.isErrorEnabled()) {
                fLog.error("(REMOVING IPLUG!) Could not retrieve query result from IPlug: " + this.fPlugId, e);
            }
            this.fRegistry.removePlug(this.fPlugId);
        } finally {
            final String plugid = this.fPlugId;
            synchronized (this.fResultSet) {
                if (this.fResultSet != null) {
                    this.fResultSet.resultsAdded();
                } else {
                    if (fLog.isErrorEnabled()) {
                        fLog.error("No ResultSet set where IPlug " + plugid + " can sent its completion.");
                    }
                }
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