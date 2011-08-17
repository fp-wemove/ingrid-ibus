/*
 * Copyright (c) 1997-2005 by media style GmbH
 * 
 * $Source: /cvs/asp-search/src/java/com/ms/aspsearch/PermissionDeniedException.java,v $
 */

package de.ingrid.ibus;

import de.ingrid.ibus.net.IPlugProxyFactory;
import de.ingrid.utils.IPlug;
import de.ingrid.utils.IngridHit;
import de.ingrid.utils.IngridHitDetail;
import de.ingrid.utils.IngridHits;
import de.ingrid.utils.PlugDescription;
import de.ingrid.utils.query.IngridQuery;

/**
 * 
 */
public class HangingPlugDummyProxyFactory implements IPlugProxyFactory {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public IPlug createPlugProxy(PlugDescription plug, String busurl) throws Exception {
        return new HangingPlug();
    }

    class HangingPlug implements IPlug {

        public void configure(PlugDescription plugDescription) throws Exception {
            // TODO Auto-generated method stub

        }

        private boolean fInitQueryPassed;

        public IngridHits search(IngridQuery query, int start, int length) throws Exception {
            if (!this.fInitQueryPassed) {
                this.fInitQueryPassed = true;
                return new IngridHits();
            }
            System.out.println("i will wait for ever");
            Thread.sleep(Integer.MAX_VALUE);
            return null;
        }

        public IngridHitDetail getDetail(IngridHit hit, IngridQuery query, String[] requestedFields) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        public IngridHitDetail[] getDetails(IngridHit[] hits, IngridQuery query, String[] requestedFields)
                throws Exception {
            // TODO Auto-generated method stub
            return null;
        }

        public void close() throws Exception {
            // TODO Auto-generated method stub

        }

        @Override
		public String toString() {
        	  if (!this.fInitQueryPassed) {
				this.fInitQueryPassed = true;
				return super.toString();
			}
			System.out.println("i will wait for ever");
			try {
				Thread.sleep(Integer.MAX_VALUE);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
    }

}